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
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
    private ServerController serverController;

    private String dbDirectory;
    private String rmiServiceName;
    private int registryPort;
    private String clientTcpPort;

    private String dbURL;
    private int dbVersion;

    private int threadNumber;
    private final ArrayList<ServerThread> serverThreadsList;

    private ServerSocket serverSocket; //TCP Client
    private MulticastSocket hearBeatSocket; //Multicast

    private ScheduledExecutorService heartBeatExecuter; //Thread for heartbeat
    private ServerService serverService; //RMI Service

    private volatile boolean isServerRunning;

    public Server() {
        this.threadNumber = 0;
        serverThreadsList = new ArrayList<>();
        isServerRunning = true;
    }

    public void setDbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
        serverController.setDbVersionLabel(this.dbVersion);
        Thread thread = new Thread(this::sendHeartbeat);
        thread.start();
    }

    public int getDbVersion() {
        return dbVersion;
    }

    public ArrayList<ServerThread> getServerThreadsList() {
        return serverThreadsList;
    }

    public void initServer(String dbDirectory, String rmiServiceName, int registryPort, String clientTcpPort, ServerController controller) {
        this.dbDirectory = dbDirectory;
        this.rmiServiceName = rmiServiceName;
        this.registryPort = registryPort;
        this.clientTcpPort = clientTcpPort;
        this.serverController = controller;

        try {
            initDB();
            initRMIService();
            startSendingHeartbeat();
            startTCPServerThreads();
        } catch (SQLException e) {
            System.err.println("[Server] DB Connection Error: " + e.getMessage());
            serverController.addToConsole("[Server] DB Connection Error: " + e.getMessage());
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("[Server] RMI Service Error: " + e.getMessage());
            serverController.addToConsole("[Server] RMI Service Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[Server] TCPServer Error: " + e.getMessage());
            serverController.addToConsole("[Server] TCPServer Error: " + e.getMessage());
        }
    }


    private void initDB() throws SQLException {
        dbURL = dbDirectory + File.separator + Constants.DB_FILE_NAME;
        File dbFile = new File(dbURL);

        if (dbFile.exists()) {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbURL);
            System.out.println("[Server] Connection Established to " + dbURL);
            serverController.addToConsole("[Server] Connection Established to " + dbURL);

            setDbVersion(EventManagerDB.getDBVersion(conn));
        } else {
            System.err.println("[Server] Database does not exist. Creating tables and admin user.");

            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbURL);
            System.out.println("[Server] Connection Established to " + dbURL);
            serverController.addToConsole("[Server] Connection Established to " + dbURL);

            // Criação de tabelas e admin
            EventManagerDB.createTables(conn);
            EventManagerDB.createAdmin(conn);
            //TODO: remover no fim dos testes
            EventManagerDB.createDummyData(conn);
        }
    }


    private void initRMIService() throws RemoteException, MalformedURLException {
        try {
            LocateRegistry.createRegistry(registryPort);
            System.out.println("[Server] Registry launch");
            serverController.addToConsole("[Server] Registry launch");
        } catch (RemoteException e) {
            System.out.println("[Server] Registry probably already in execution");
        }

        serverService = new ServerService(new File(dbDirectory), Constants.DB_FILE_NAME, serverController);
        System.out.println("[Server] Servico GetRemoteFile criado e em execucao: " + serverService.getRef().remoteToString());
        serverController.addToConsole("[Server] Servico GetRemoteFile criado e em execucao");

        Naming.rebind("rmi://localhost/" + rmiServiceName, serverService);
        System.out.println("[Server] Servico " + rmiServiceName + " registado no registry...");
        serverController.addToConsole("[Server] Servico " + rmiServiceName + " registado no registry...");

        serverController.setRMIServiceOnline(true);
    }

    private void stopRMIService() {
        if (serverService != null) {
            try {
                Naming.unbind("rmi://localhost/" + rmiServiceName);
                UnicastRemoteObject.unexportObject(serverService, true);
                System.out.println("[Server] Servico " + rmiServiceName + " desligado.");
                serverController.addToConsole("[Server] Servico " + rmiServiceName + " desligado.");
                serverController.setRMIServiceOnline(false);

            } catch (RemoteException | MalformedURLException | NotBoundException e) {
                System.out.println("[Server] Error stopping RMI Service: " + e.getMessage());
                serverController.addToConsole("[Server] Error stopping RMI Service: " + e.getMessage());
            }
        }
    }


    private void startSendingHeartbeat() {
        heartBeatExecuter = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            return thread;
        });

        heartBeatExecuter.scheduleAtFixedRate(this::sendHeartbeat, 0, 10, TimeUnit.SECONDS);
        serverController.setHeartBeatServiceOnline(true);
    }

    private void stopSendingHeartbeat() {
        if (heartBeatExecuter != null) {
            heartBeatExecuter.shutdown();
            System.out.println("[Server] HeartBeat desligado");
            serverController.addToConsole("[Server] HeartBeat desligado");
            serverController.setHeartBeatServiceOnline(false);
        }
    }

    private void sendHeartbeat() {
        try {
            InetAddress group = InetAddress.getByName(Constants.HEARTBEAT_URL);
            NetworkInterface nif;
            DatagramPacket dgram;

            nif = NetworkInterface.getByInetAddress(InetAddress.getByName(Constants.HEARTBEAT_URL));


            hearBeatSocket = new MulticastSocket(Constants.HEARTBEAT_PORT);
            hearBeatSocket.joinGroup(new InetSocketAddress(group, Constants.HEARTBEAT_PORT), nif);

            HeartBeatMsg msg = createHeartbeatMessage();

            if (msg == null) {
                System.out.println("[Server] Error creating heartbeat msg ");
                serverController.addToHeartBeatConsole("[Server] Error creating heartbeat msg");
                return;
            }

            try (ByteArrayOutputStream buff = new ByteArrayOutputStream();
                 ObjectOutputStream out = new ObjectOutputStream(buff)) {
                out.writeObject(msg);

                dgram = new DatagramPacket(buff.toByteArray(), buff.size(), group, Constants.HEARTBEAT_PORT);
            }
            hearBeatSocket.send(dgram);

            System.out.println("[Server] Mensagem de heartbeat enviada para " + Constants.HEARTBEAT_URL + ":" + Constants.HEARTBEAT_PORT);
            serverController.addToHeartBeatConsole("[Server] Mensagem de heartbeat enviada para " + Constants.HEARTBEAT_URL + ":" + Constants.HEARTBEAT_PORT);
        } catch (IOException e) {
            System.out.println("[Server] Error creating heartbeat: " + e.getMessage());
            serverController.addToHeartBeatConsole("[Server] Error creating heartbeat: " + e.getMessage());
        } finally {
            if (hearBeatSocket != null) {
                hearBeatSocket.close();
            }
        }
    }

    private HeartBeatMsg createHeartbeatMessage() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            return new HeartBeatMsg(registryPort, rmiServiceName, dbVersion, ip);
        } catch (UnknownHostException e) {
            System.out.println("[Server] Error creating hearBeat msg: " + e.getMessage());
            serverController.addToHeartBeatConsole("[Server] Error creating hearBeat msg: " + e.getMessage());
            return null;
        }
    }


    private void startTCPServerThreads() throws IOException {
        serverSocket = new ServerSocket(Integer.parseInt(clientTcpPort));

        System.out.println("[Server] TCP Server inicialized in port " + serverSocket.getLocalPort() + " ...");
        serverController.addToConsole("[Server] TCP Server inicialized in port " + serverSocket.getLocalPort() + " ...");

        ServerThread serverThread;
        Socket toClientSocket = null;

        while (isServerRunning) {
            try {
                toClientSocket = serverSocket.accept();
                serverThread = new ServerThread(toClientSocket, dbURL, serverController, this, serverService);
                serverThread.setDaemon(true);

                serverThreadsList.add(serverThread);

                serverThread.start();
            } catch (Exception e) {
                System.out.println("[Server] Error in TCPServer: " + e.getMessage());
                serverController.addToConsole("[Server] Error in TCPServer: " + e.getMessage());
                if (serverSocket != null) {
                    serverSocket.close();
                }
                break;
            }
        }

        if (toClientSocket != null) {
            toClientSocket.close();
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    private void stopTCPServerThreads() {
        for (ServerThread serverThread : serverThreadsList) {
            serverThread.stopServerThread();
        }
    }

    public void stopServer() {
        isServerRunning = false;
        stopTCPServerThreads();
        stopSendingHeartbeat();
        stopRMIService();
    }

    public boolean serverServiceGetDBFileIsRunning() {
        return serverService.isGetDBFileRunning();
    }

    public void refreshClientEvents() {
        ArrayList<ServerThread> unresponsiveThreads = new ArrayList<>();

        for (ServerThread serverThread : serverThreadsList) {
            if (serverThread.isContinuosCommunication())
                try {
                    serverThread.refreshClientEvents();
                } catch (IOException e) {
                    System.out.println("[server] Error refreshing client events, removing client...");
                    serverController.addToConsole("[server] Error refreshing client events, removing client...");
                    unresponsiveThreads.add(serverThread);
                }
        }

        serverThreadsList.removeAll(unresponsiveThreads);
    }

    public void refreshClientAttendances(int eventId) {
        ArrayList<ServerThread> unresponsiveThreads = new ArrayList<>();

        for (ServerThread serverThread : serverThreadsList) {
            if (serverThread.isContinuosCommunication())
                try {
                    serverThread.refreshClientAttendances(eventId);
                } catch (IOException e) {
                    System.out.println("[server] Error refreshing client attendances, removing client...");
                    serverController.addToConsole("[server] Error refreshing client attendances, removing client...");
                    unresponsiveThreads.add(serverThread);
                }
        }

        serverThreadsList.removeAll(unresponsiveThreads);
    }
}
