package pt.isec.eventmanager.serverBackup;

import pt.isec.eventmanager.db.EventManagerDB;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.heartBeat.HeartBeatMsg;
import pt.isec.eventmanager.heartBeat.HeartBeatThread;
import pt.isec.eventmanager.rmi.BackupServerService;
import pt.isec.eventmanager.rmi.ServerServiceInterface;
import pt.isec.eventmanager.rmi.ServerServiceObserver;
import pt.isec.eventmanager.users.User;
import pt.isec.eventmanager.util.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ServerBackup {
    private final String dbDirectory;
    private String dbUrl;

    private HeartBeatThread heartbeatListenerThread;
    private HeartBeatMsg heartBeatMsg;

    private final ServerBackupController serverBackupController;

    private ServerServiceInterface serverServiceInterface;

    private ServerServiceObserver observer;

    private boolean firstRun = true;
    private boolean update = false;

    private int dbVersion;

    private final Object lock = new Object();

    public ServerBackup(String dbDirectory, ServerBackupController controller) {
        this.serverBackupController = controller;
        this.dbDirectory = dbDirectory;
    }

    private void startServerBackup() {
        if (heartBeatMsg.getServerRMIServiceName().isEmpty()) return;

        try {
            initServerService();
            getDBFile();
            startObserver();

            System.out.println("[ServerBackup] DBFile saved in " + dbDirectory);
            serverBackupController.addToConsole("[ServerBackup] DBFile saved in " + dbDirectory);

            dbVersion = heartBeatMsg.getDbVersion();
            serverBackupController.setDbVersionLabel(dbVersion);

            firstRun = false;
        } catch (NotBoundException | IOException e) {
            System.out.println("[ServerBackup] Error Getting DBFile: " + e.getMessage());
            serverBackupController.addToConsole("[ServerBackup] Error Getting DBFile: " + e.getMessage());
        }
    }

    public void stopServerBackup() {
        stopHeartBeatLookup();
        stopObserver();
    }

    public void startHeartBeatLookup() {
        System.out.println("[ServerBackup] Starting heartbeat lookup");
        try {
            InetAddress group = InetAddress.getByName(Constants.HEARTBEAT_URL);
            NetworkInterface nif = null;

            try {
                nif = NetworkInterface.getByInetAddress(InetAddress.getByName(Constants.HEARTBEAT_URL));
            } catch (SocketException | NullPointerException | UnknownHostException | SecurityException ex) {
                System.out.println("[ServerBackup] Error creating NetworkInterface");
                serverBackupController.addToConsole("[ServerBackup] Error creating NetworkInterface");
                return;
            }

            MulticastSocket socket = new MulticastSocket(Constants.HEARTBEAT_PORT);
            socket.joinGroup(new InetSocketAddress(group, Constants.HEARTBEAT_PORT), nif);

            heartbeatListenerThread = new HeartBeatThread(socket, serverBackupController, this);
            heartbeatListenerThread.setDaemon(true);
            heartbeatListenerThread.start();

        } catch (IOException e) {
            System.out.println("[ServerBackup] Error starting HeartBeat Lookup" + e.getMessage());
            serverBackupController.addToConsole("[ServerBackup] Error starting HeartBeat Lookup" + e.getMessage());
        }
    }

    private void stopHeartBeatLookup() {
        if (heartbeatListenerThread != null) {
            heartbeatListenerThread.stopHeartBeatThread();
            heartbeatListenerThread.interrupt();
        }
    }

    public void setHearBeatMsgReceived(HeartBeatMsg msg) {
        this.heartBeatMsg = msg;

        if (firstRun) {
            startServerBackup();
        }
    }

    private void initServerService() throws MalformedURLException, NotBoundException, RemoteException {
        serverServiceInterface = (ServerServiceInterface) Naming.lookup(heartBeatMsg.getServerRMIServiceName());
    }

    private void getDBFile() throws IOException {
        String localFilePath = new File(dbDirectory + File.separator + Constants.DB_FILE_NAME).getCanonicalPath();

        FileOutputStream localFileOutputStream = new FileOutputStream(localFilePath);

        BackupServerService backupServerService = new BackupServerService();

        backupServerService.setFout(localFileOutputStream);
        serverServiceInterface.getDBFile(backupServerService);

        this.dbUrl = dbDirectory + File.separator + Constants.DB_FILE_NAME;
    }

    private void startObserver() throws RemoteException {
        observer = new ServerServiceObserver(this);
        System.out.println("[ServerBackup] Servico ServerServiceObserver criado e em execucao...");
        serverBackupController.addToConsole("[ServerBackup] Servico ServerServiceObserver criado e em execucao...");
        serverServiceInterface.addObserver(observer);
    }

    private void stopObserver() {
        if (serverServiceInterface == null || observer == null) return;

        System.out.println("[ServerBackup] Stopping Observer");
        serverBackupController.addToConsole("[ServerBackup] Stopping Observer");
        try {
            serverServiceInterface.removeObserver(observer);
            UnicastRemoteObject.unexportObject(observer, true);
        } catch (RemoteException e) {
            System.out.println("[ServerBackup] Error Stopping Observer: " + e.getMessage());
            serverBackupController.addToConsole("[ServerBackup] Error Stopping Observer: " + e.getMessage());
        }
    }

    //OBSERVER
    public void observerNotify(String description) {
        System.out.println("[ServerBackup] " + description);
        serverBackupController.addToConsole("[ServerBackup] " + description);
    }

    public void insertUser(int dbVersion, User user) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        synchronized (lock) {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
            System.out.println("[ServerBackup] Inserting User");
            serverBackupController.addToConsole("[ServerBackup] Inserting User");

            boolean success = EventManagerDB.insertUser(conn, user);
            incrementDBVersion(success, conn);

            conn.close();
        }
    }

    public void editUser(int dbVersion, User user) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        synchronized (lock) {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
            System.out.println("[ServerBackup] Editing User");
            serverBackupController.addToConsole("[ServerBackup] Editing User");

            boolean success = EventManagerDB.editUser(conn, user);
            incrementDBVersion(success, conn);

            conn.close();
        }
    }

    public void insertEvent(int dbVersion, Event event) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        synchronized (lock) {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
            System.out.println("[ServerBackup] Inserting Event");
            serverBackupController.addToConsole("[ServerBackup] Inserting Event");

            boolean success = EventManagerDB.insertEvent(conn, event);
            incrementDBVersion(success, conn);

            conn.close();
        }
    }

    public void deleteEvent(int dbVersion, Event event) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        synchronized (lock) {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
            System.out.println("[ServerBackup] Deleting Event");
            serverBackupController.addToConsole("[ServerBackup] Deleting Event");


            boolean success = EventManagerDB.deleteEvent(conn, event);
            incrementDBVersion(success, conn);

            conn.close();
        }
    }

    public void editEvent(int dbVersion, Event event) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        synchronized (lock) {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
            System.out.println("[ServerBackup] Editing Event");
            serverBackupController.addToConsole("[ServerBackup] Editing Event");

            boolean success = EventManagerDB.editEvent(conn, event);
            incrementDBVersion(success, conn);

            conn.close();
        }
    }

    public void insertAttendance(int dbVersion, int eventId, String username) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        synchronized (lock) {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
            System.out.println("[ServerBackup] Inserting Attendance");
            serverBackupController.addToConsole("[ServerBackup] Inserting Attendance");

            boolean success = EventManagerDB.insertAttendanceEvent(conn, eventId, username);
            incrementDBVersion(success, conn);

            conn.close();
        }
    }

    public void deleteAttendance(int dbVersion, int eventId, String username) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        synchronized (lock) {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
            System.out.println("[ServerBackup] Deleting Attendance");
            serverBackupController.addToConsole("[ServerBackup] Deleting Attendance");

            boolean success = EventManagerDB.deletePresenceFromEvent(conn, eventId, username);
            incrementDBVersion(success, conn);

            conn.close();
        }
    }

    public void insertEventKey(int dbVersion, EventKey eventKey) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        synchronized (lock) {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
            System.out.println("[ServerBackup] Inserting Event Key");
            serverBackupController.addToConsole("[ServerBackup] Inserting Event Key");


            boolean success = EventManagerDB.insertEventKey(conn, eventKey);
            incrementDBVersion(success, conn);

            conn.close();
        }
    }

    public void deleteEventKey(int dbVersion, EventKey eventKey) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        synchronized (lock) {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
            System.out.println("[ServerBackup] Deleting Event Key");
            serverBackupController.addToConsole("[ServerBackup] Deleting Event Key");

            boolean success = EventManagerDB.deleteEventKey(conn, eventKey);
            incrementDBVersion(success, conn);

            conn.close();
        }
    }

    //DB
    private void incrementDBVersion(boolean increment, Connection conn) throws SQLException {
        if (!increment) {
            stopServerBackup();
        }

        int newDBVersion = EventManagerDB.incrementDBVersion(conn);

        if (newDBVersion > 0) {
            System.out.println("[ServerBackup] DBVersion Increment Success");
            serverBackupController.addToConsole("[ServerBackup] DBVersion Increment Success");
            dbVersion = newDBVersion;
            serverBackupController.setDbVersionLabel(dbVersion);
        } else {
            System.out.println("[ServerBackup] DBVersion Increment Error");
            serverBackupController.addToConsole("[ServerBackup] DBVersion Increment Error");
            stopServerBackup();
        }
    }

    private boolean dbVersionCeck(int dbVersion) {
        return this.dbVersion != dbVersion;
    }
}
