package pt.isec.eventmanager.server;

import pt.isec.eventmanager.db.EventManagerDB;
import pt.isec.eventmanager.heartBeat.HeartBeatMsg;
import pt.isec.eventmanager.rmi.ServerService;
import pt.isec.eventmanager.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
    private ServerController serverController;

    private String clientTcpPort;
    private int registryPort;
    private String rmiServiceName;

    private String dbDirectory;
    private int dbVersion;
    private int threadNumber;

    private ServerSocket serverSocket;
    private MulticastSocket hearBeatSocket;

    private ScheduledExecutorService heartBeatExecuter;

    private ServerService serverService;


    public Server() {
        this.threadNumber = 0;
    }

    public void incrementDbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
        Thread thread = new Thread(this::sendHeartbeat);
        thread.start();
    }

    public void initServer(String clientTcpPort, String dbDirectory, int registryPort, String rmiServiceName, ServerController controller) {
        this.serverController = controller;
        this.clientTcpPort = clientTcpPort;
        this.dbDirectory = dbDirectory;
        this.registryPort = registryPort;
        this.rmiServiceName = rmiServiceName;

        initDB();
        startSendingHeartbeat();
        initRMIService();

        try {
            initTcpServer();
        } catch (IOException e) {
            System.out.println("[Server] Error closing ServerSocket: " + e.getMessage());
            serverController.addToConsole("[Server] Error closing ServerSocket: " + e.getMessage());
        }
    }

    private void initDB() {
        String dbURL = dbDirectory + File.separator + Constants.DB_FILE_NAME;
        File dbFile = new File(dbURL);

        if (dbFile.exists()) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbURL)) {
                System.out.println("[Server] Connection Established to " + dbURL);
                serverController.addToConsole("[Server] Connection Established to " + dbURL);

                this.dbVersion = EventManagerDB.getDBVersion(conn, serverController);

            } catch (SQLException e) {
                System.err.println("[Server] Connection Error: " + e.getMessage());
                serverController.addToConsole("[Server] Connection Error: " + e.getMessage());
            }
        } else {
            System.err.println("[Server] Database does not exist. Creating tables and admin user.");

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbURL)) {
                System.out.println("[Server] Connection Established to " + dbURL);
                serverController.addToConsole("[Server] Connection Established to " + dbURL);

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

    private void initTcpServer() throws IOException {
        try {
            serverSocket = new ServerSocket(Integer.parseInt(clientTcpPort));

            System.out.println("[Server] TCP Time Server inicialized in port " + serverSocket.getLocalPort() + " ...");
            serverController.addToConsole("[Server] TCP Time Server inicialized in port " + serverSocket.getLocalPort() + " ...");

            while (true) {
                try {
                    Socket toClientSocket = serverSocket.accept();
                    Thread t = new ServerThread(toClientSocket, threadNumber++, dbDirectory, serverController, this);
                    t.setDaemon(true);
                    t.start();
                } catch (Exception e) {
                    System.out.println("[Server] Problem communication with client: " + e.getMessage());
                    serverController.addToConsole("[Server] Problem communication with client: " + e.getMessage());
                }
            }

        } catch (NumberFormatException e) {
            System.out.println("[Server] Port number must be a positive integer!");
            serverController.addToConsole("[Server] Port number must be a positive integer!");
        } finally {
            if (serverSocket != null)
                serverSocket.close();
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

            hearBeatSocket = new MulticastSocket(Constants.HEARTBEAT_PORT);
            hearBeatSocket.joinGroup(new InetSocketAddress(group, Constants.HEARTBEAT_PORT), nif);

            try (ByteArrayOutputStream buff = new ByteArrayOutputStream();
                 ObjectOutputStream out = new ObjectOutputStream(buff)) {
                out.writeObject(createHeartbeatMessage());

                dgram = new DatagramPacket(buff.toByteArray(), buff.size(), group, Constants.HEARTBEAT_PORT);
            }
            hearBeatSocket.send(dgram);

            System.out.println("[Server] Mensagem de heartbeat enviada para " + Constants.HEARTBEAT_URL + ":" + Constants.HEARTBEAT_PORT);
            serverController.addToConsole("[Server] Mensagem de heartbeat enviada para " + Constants.HEARTBEAT_URL + ":" + Constants.HEARTBEAT_PORT);
        } catch (IOException e) {
            System.out.println("[Server] Error creating heartbeat: " + e.getMessage());
            serverController.addToConsole("[Server] Error creating heartbeat: " + e.getMessage());
        } finally {
            if (hearBeatSocket != null) {
                hearBeatSocket.close();
            }
        }
    }

    private HeartBeatMsg createHeartbeatMessage() {
        return new HeartBeatMsg(registryPort, rmiServiceName, dbVersion);
    }

    private void initRMIService() {
        try {
            try {
                LocateRegistry.createRegistry(registryPort);
            } catch (RemoteException e) {
                System.out.println("[Server] Error creating ServerService: " + e.getMessage());
            }

            serverService = new ServerService(new File(dbDirectory), Constants.DB_FILE_NAME);
            System.out.println("[Server] Servico GetRemoteFile criado e em execucao: " + serverService.getRef().remoteToString());

            Naming.rebind("rmi://localhost/" + rmiServiceName, serverService);
            System.out.println("[Server] Servico " + rmiServiceName + " registado no registry...");

        } catch (RemoteException | MalformedURLException e) {
            System.out.println("[Server] Error creating RMI Service: " + e.getMessage());
            serverController.addToConsole("[Server] Error creating RMI Service: " + e.getMessage());
        }
    }

    public void stopRMIService() {
        if (serverService != null) {
            try {
                Naming.unbind("rmi://localhost/" + rmiServiceName);
                UnicastRemoteObject.unexportObject(serverService, true);
                System.out.println("[Server] Servico " + rmiServiceName + " desligado.");

            } catch (RemoteException | MalformedURLException | NotBoundException e) {
                System.out.println("[Server] Error stopping RMI Service: " + e.getMessage());
                serverController.addToConsole("[Server] Error stopping RMI Service: " + e.getMessage());
            }
        }
    }
}
