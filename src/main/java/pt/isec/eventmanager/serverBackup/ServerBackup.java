package pt.isec.eventmanager.serverBackup;

import pt.isec.eventmanager.heartBeat.HeartBeatMsg;
import pt.isec.eventmanager.heartBeat.HeartBeatThread;
import pt.isec.eventmanager.rmi.BackupServerService;
import pt.isec.eventmanager.rmi.ServerServiceInterface;
import pt.isec.eventmanager.util.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;

public class ServerBackup {
    private String dbDirectory;

    private HeartBeatThread heartbeatListenerThread;
    private HeartBeatMsg heartBeatMsg;

    private InetAddress group;
    private MulticastSocket socket = null;
    private NetworkInterface nif;

    private ServerServiceInterface serverServiceInterface;
    private BackupServerService backupServerService;

    public ServerBackup(String dbDirectory) {
        this.dbDirectory = dbDirectory;
    }

    public void startHeartBeatLookup(ServerBackupController controller) {
        System.out.println("[ServerBackup] Stopping heartbeat");
        try {
            group = InetAddress.getByName(Constants.HEARTBEAT_URL);
            nif = null;

            try {
                nif = NetworkInterface.getByInetAddress(InetAddress.getByName(Constants.HEARTBEAT_URL));
            } catch (SocketException | NullPointerException | UnknownHostException | SecurityException ex) {
                System.out.println("[ServerBackup] Error creating NetworkInterface");
                return;
            }

            socket = new MulticastSocket(Constants.HEARTBEAT_PORT);
            socket.joinGroup(new InetSocketAddress(group, Constants.HEARTBEAT_PORT), nif);

            //Lanca a thread adicional dedicada a aguardar por datagramas no socket e a processá-los
            heartbeatListenerThread = new HeartBeatThread(socket, controller, this);
            heartbeatListenerThread.setDaemon(true);
            heartbeatListenerThread.start();
        } catch (IOException e) {
            System.out.println("[ServerBackup] Error starting HeartBeat Lookup" + e.getMessage());
        }
    }

    public void stopHeartBeatLookup() {
        socket.close();
        heartbeatListenerThread.interrupt();
    }

    public void setHearBeatMsg(HeartBeatMsg msg) {
        this.heartBeatMsg = msg;
        //TODO: ver se a dbversion ainda é a mesma...
        try {
            starRMIService();
        } catch (NotBoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void starRMIService() throws IOException, NotBoundException {
        if (heartBeatMsg.getServerRMIServiceName().isEmpty()) return;

        serverServiceInterface = (ServerServiceInterface) Naming.lookup(heartBeatMsg.getServerRMIServiceName());

        String localFilePath = new File(dbDirectory + File.separator + Constants.DB_FILE_NAME).getCanonicalPath();

        try (FileOutputStream localFileOutputStream = new FileOutputStream(localFilePath)) {

            backupServerService = new BackupServerService();

            backupServerService.setFout(localFileOutputStream);
            serverServiceInterface.getDBFile(backupServerService);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (backupServerService != null) {
                backupServerService.setFout(null);
                try {
                    UnicastRemoteObject.unexportObject(backupServerService, true);
                } catch (NoSuchObjectException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
