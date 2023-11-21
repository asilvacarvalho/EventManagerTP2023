package pt.isec.eventmanager.serverBackup;

import pt.isec.eventmanager.heartBeat.HeartBeatThread;
import pt.isec.eventmanager.util.Constants;

import java.io.IOException;
import java.net.*;

public class ServerBackup {
    protected HeartBeatThread heartbeatListenerThread;

    protected InetAddress group;
    protected MulticastSocket socket = null;
    protected NetworkInterface nif;

    public ServerBackup() {
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
            heartbeatListenerThread = new HeartBeatThread(socket, controller);
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
}
