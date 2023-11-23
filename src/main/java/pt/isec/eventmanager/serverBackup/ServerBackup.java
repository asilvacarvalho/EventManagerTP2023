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
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ServerBackup {
    private String dbDirectory;
    private String dbUrl;

    private HeartBeatThread heartbeatListenerThread;
    private HeartBeatMsg heartBeatMsg;

    private InetAddress group;
    private MulticastSocket socket = null;
    private NetworkInterface nif;

    private ServerBackupController serverBackupController;

    private ServerServiceInterface serverServiceInterface;
    private BackupServerService backupServerService;

    private ServerServiceObserver observer;

    private boolean firstRun = true;
    private boolean update = false;

    private int dbVersion;

    private final Object lock = new Object();

    public ServerBackup(String dbDirectory, ServerBackupController controller) {
        this.serverBackupController = controller;
        this.dbDirectory = dbDirectory;
    }

    public void startServerBackup() {
        startHeartBeatLookup();
    }

    public void stopServerBackup() {
        stopHeartBeatLookup();
        stopObserver();
    }

    private void startHeartBeatLookup() {
        System.out.println("[ServerBackup] Stopping heartbeat");
        try {
            group = InetAddress.getByName(Constants.HEARTBEAT_URL);
            nif = null;

            try {
                nif = NetworkInterface.getByInetAddress(InetAddress.getByName(Constants.HEARTBEAT_URL));
            } catch (SocketException | NullPointerException | UnknownHostException | SecurityException ex) {
                System.out.println("[ServerBackup] Error creating NetworkInterface");
                serverBackupController.addToConsole("[ServerBackup] Error creating NetworkInterface");
                return;
            }

            socket = new MulticastSocket(Constants.HEARTBEAT_PORT);
            socket.joinGroup(new InetSocketAddress(group, Constants.HEARTBEAT_PORT), nif);

            //Lanca a thread adicional dedicada a aguardar por datagramas no socket e a processá-los
            heartbeatListenerThread = new HeartBeatThread(socket, serverBackupController, this);
            heartbeatListenerThread.setDaemon(true);
            heartbeatListenerThread.start();
        } catch (IOException e) {
            System.out.println("[ServerBackup] Error starting HeartBeat Lookup" + e.getMessage());
            serverBackupController.addToConsole("[ServerBackup] Error creating NetworkInterface");
        }
    }

    private void stopHeartBeatLookup() {
        socket.close();
        heartbeatListenerThread.interrupt();
    }

    public void setHearBeatMsgReceived(HeartBeatMsg msg) {
        this.heartBeatMsg = msg;

        if (firstRun || update) {
            try {
                initServerService();
                addObserver();

                getDBFile();

                System.out.println("[ServerBackup] DBFile saved in " + dbDirectory);
                serverBackupController.addToConsole("[ServerBackup] DBFile saved in " + dbDirectory);

                this.dbVersion = msg.getDbVersion();
                serverBackupController.setDbVersionLabel(this.dbVersion);

                firstRun = false;
                update = false;
            } catch (NotBoundException | IOException e) {
                System.out.println("[ServerBackup] Error Getting DBFile: " + e.getMessage());
                serverBackupController.addToConsole("[ServerBackup] Error Getting DBFile: " + e.getMessage());
            }
        }
    }

    private void initServerService() throws MalformedURLException, NotBoundException, RemoteException {
        serverServiceInterface = (ServerServiceInterface) Naming.lookup(heartBeatMsg.getServerRMIServiceName());
    }

    private void getDBFile() throws IOException, NotBoundException {
        if (heartBeatMsg.getServerRMIServiceName().isEmpty()) return;

        String localFilePath = new File(dbDirectory + File.separator + Constants.DB_FILE_NAME).getCanonicalPath();

        try (FileOutputStream localFileOutputStream = new FileOutputStream(localFilePath)) {

            backupServerService = new BackupServerService();
            serverBackupController.setRMIServiceOnline(true);

            backupServerService.setFout(localFileOutputStream);
            serverServiceInterface.getDBFile(backupServerService);

            this.dbUrl = dbDirectory + File.separator + Constants.DB_FILE_NAME;

        } finally {
            if (backupServerService != null) {
                backupServerService.setFout(null);
                try {
                    UnicastRemoteObject.unexportObject(backupServerService, true);
                    serverBackupController.setRMIServiceOnline(false);
                } catch (NoSuchObjectException e) {
                    System.out.println("[ServerBackup] Error Removing RMI Service: " + e.getMessage());
                    serverBackupController.addToConsole("[ServerBackup] Error Removing RMI Service: " + e.getMessage());
                }
            }
        }
    }

    private void addObserver() throws RemoteException {
        observer = new ServerServiceObserver(this);
        System.out.println("[ServerBackup] Servico ServerServiceObserver criado e em execucao...");
        serverBackupController.addToConsole("[ServerBackup] Servico ServerServiceObserver criado e em execucao...");
        serverServiceInterface.addObserver(observer);
    }

    private void stopObserver() {
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
        System.out.println("[ServerServiceObserver] " + description);
        serverBackupController.addToConsole("[ServerServiceObserver] " + description);
    }

    public void insertUser(int dbVersion, User user) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverBackupController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.insertUser(conn, user);
            incrementDBVersion(success);
        }
    }

    public void editUser(int dbVersion, User user) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverBackupController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.editUser(conn, user);
            incrementDBVersion(success);
        }
    }

    public void insertEvent(int dbVersion, Event event) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverBackupController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.insertEvent(conn, event);
            incrementDBVersion(success);
        }
    }

    public void deleteEvent(int dbVersion, Event event) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverBackupController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.deleteEvent(conn, event);
            incrementDBVersion(success);
        }
    }

    public void editEvent(int dbVersion, Event event) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverBackupController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.editEvent(conn, event);
            incrementDBVersion(success);
        }
    }

    public void insertAttendance(int dbVersion, int eventId, String username) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverBackupController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.insertAttendanceEvent(conn, eventId, username);
            incrementDBVersion(success);
        }
    }

    public void deleteAttendance(int dbVersion, int eventId, String username) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverBackupController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.deletePresenceFromEvent(conn, eventId, username);
            incrementDBVersion(success);
        }
    }

    public void insertEventKey(int dbVersion, EventKey eventKey) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverBackupController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.insertEventKey(conn, eventKey);
            incrementDBVersion(success);
        }
    }

    public void deleteEventKey(int dbVersion, EventKey eventKey) throws SQLException {
        if (dbVersionCeck(dbVersion)) {
            stopObserver();
            return;
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverBackupController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.deleteEventKey(conn, eventKey);
            incrementDBVersion(success);
        }
    }

    //DB
    private void incrementDBVersion(boolean increment) throws SQLException {
        if (!increment) {
            stopServerBackup();
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverBackupController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        int newDBVersion = EventManagerDB.incrementDBVersion(conn);

        if (newDBVersion > 0) {
            System.out.println("[ServerThread] DBVersion Increment Success");
            serverBackupController.addToConsole("[ServerThread] DBVersion Increment Success");
            this.dbVersion = newDBVersion;
        } else {
            System.out.println("[ServerThread] DBVersion Increment Error");
            serverBackupController.addToConsole("[ServerThread] DBVersion Increment Error");
            stopServerBackup();
        }
    }

    private boolean dbVersionCeck(int dbVersion) {
        return this.dbVersion != dbVersion;
    }
}
