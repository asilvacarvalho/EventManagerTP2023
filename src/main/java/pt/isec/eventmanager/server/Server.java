package pt.isec.eventmanager.server;

import pt.isec.eventmanager.db.EventManagerDB;
import pt.isec.eventmanager.heartBeat.HeartBeatMsg;
import pt.isec.eventmanager.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
    private ServerController serverController;
    private String clientTcpPort;
    private String dbUrl;

    private int registryPort;
    private String rmiServiceName;

    private int dbVersion;
    private int threadNumber;

    private ServerSocket serverSocket;
    private ScheduledExecutorService heartBeatExecuter;

    MulticastSocket socket;

    public Server() {
        this.threadNumber = 0;
    }

    public void incrementDbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
        Thread thread = new Thread(this::sendHeartbeat);
        thread.start();
    }

    public void initServer(String clientTcpPort, String dbUrl, int registryPort, String rmiServiceName, ServerController controller) {
        this.serverController = controller;
        this.clientTcpPort = clientTcpPort;
        this.dbUrl = dbUrl;
        this.registryPort = registryPort;
        this.rmiServiceName = rmiServiceName;
        initDB();
        initTcpServer();


    }

    private void initDB() {
        File dbFile = new File(dbUrl);

        if (dbFile.exists()) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[Server] Connection Established to " + dbUrl);
                serverController.addToConsole("[Server] Connection Established to " + dbUrl);
                this.dbVersion = EventManagerDB.getDBVersion(conn, serverController);
            } catch (SQLException e) {
                System.err.println("[Server] Connection Error: " + e.getMessage());
                serverController.addToConsole("[Server] Connection Error: " + e.getMessage());
            }
        } else {
            System.err.println("[Server] Database does not exist. Creating tables and admin user.");

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[Server] Connection Established to " + dbUrl);
                serverController.addToConsole("[Server] Connection Established to " + dbUrl);

                // Criação de tabelas e admin
                EventManagerDB.createTables(conn, serverController);
                EventManagerDB.createAdmin(conn, serverController);
                //TODO: remover no fim dos testes
                EventManagerDB.createDummyData(conn, serverController);

            } catch (SQLException e) {
                System.err.println("[Server] Connection Error: " + e.getMessage());
                serverController.addToConsole("[Server] Connection Error: " + e.getMessage());
            }
        }
    }

    private void initTcpServer() {
        try (ServerSocket socket = new ServerSocket(Integer.parseInt(clientTcpPort))) {
            this.serverSocket = socket;
            System.out.println("[Server] TCP Time Server inicialized in port " + socket.getLocalPort() + " ...");
            serverController.addToConsole("[Server] TCP Time Server inicialized in port " + socket.getLocalPort() + " ...");

            startSendingHeartbeat();

            while (true) {
                try {
                    Socket toClientSocket = socket.accept();
                    Thread t = new ServerThread(toClientSocket, threadNumber++, dbUrl, serverController, this);

                    t.start();
                } catch (Exception e) {
                    System.out.println("[Server] Problem communication with client: " + e.getMessage());
                    serverController.addToConsole("[Server] Problem communication with client: " + e.getMessage());
                }
            }

        } catch (NumberFormatException e) {
            System.out.println("[Server] Port number must be a positive integer!");
            serverController.addToConsole("[Server] Port number must be a positive integer!");
        } catch (IOException e) {
            System.out.println("[Server] Error on the socket: " + e.getMessage());
            serverController.addToConsole("[Server] Error on the socket: " + e.getMessage());
        }
    }

    public void startSendingHeartbeat() {
        heartBeatExecuter = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            return thread;
        });

        heartBeatExecuter.scheduleAtFixedRate(this::sendHeartbeat, 0, 10, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        try {
            InetAddress group = InetAddress.getByName(Constants.HEARTBEAT_URL);
            NetworkInterface nif;
            DatagramPacket dgram;

            try {
                nif = NetworkInterface.getByInetAddress(InetAddress.getByName(Constants.HEARTBEAT_URL));
            } catch (SocketException | NullPointerException | UnknownHostException | SecurityException ex) {
                return;
            }

            socket = new MulticastSocket(Constants.HEARTBEAT_PORT);
            socket.joinGroup(new InetSocketAddress(group, Constants.HEARTBEAT_PORT), nif);

            try (ByteArrayOutputStream buff = new ByteArrayOutputStream();
                 ObjectOutputStream out = new ObjectOutputStream(buff)) {
                out.writeObject(createHeartbeatMessage());

                dgram = new DatagramPacket(buff.toByteArray(), buff.size(), group, Constants.HEARTBEAT_PORT);
            }
            socket.send(dgram);

            System.out.println("[Server] Mensagem de heartbeat enviada para " + Constants.HEARTBEAT_URL + ":" + Constants.HEARTBEAT_PORT);
            serverController.addToConsole("[Server] Mensagem de heartbeat enviada para " + Constants.HEARTBEAT_URL + ":" + Constants.HEARTBEAT_PORT);
        } catch (IOException e) {
            System.out.println("[Server] Error creating heartbeat: " + e.getMessage());
            serverController.addToConsole("[Server] Error creating heartbeat: " + e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private HeartBeatMsg createHeartbeatMessage() {
        return new HeartBeatMsg(registryPort, rmiServiceName, dbVersion);
    }

    private void initRMIService() {
//        try {
//            BackupService backupService = new BackupService();
//            Registry registry = LocateRegistry.createRegistry(registryPort);
//
//            registry.rebind(rmiServiceName, backupService);
//            System.out.println("RMI service '" + rmiServiceName + "' registered.");
//
//            // ... (resto do código)
//        } catch (Exception e) {
//            // Trate as exceções adequadamente
//        }
    }
}
