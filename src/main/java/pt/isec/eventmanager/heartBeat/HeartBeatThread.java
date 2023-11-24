package pt.isec.eventmanager.heartBeat;

import javafx.application.Platform;
import pt.isec.eventmanager.serverBackup.ServerBackup;
import pt.isec.eventmanager.serverBackup.ServerBackupController;
import pt.isec.eventmanager.util.Constants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

public class HeartBeatThread extends Thread {
    private MulticastSocket multicastSocket;
    private ServerBackupController controller;
    private ServerBackup serverBackup;
    private boolean running;

    public HeartBeatThread(MulticastSocket multicastSocket, ServerBackupController controller, ServerBackup serverBackup) {
        this.multicastSocket = multicastSocket;
        this.controller = controller;
        this.serverBackup = serverBackup;
        running = true;
    }

    public void stopHeartBeatThread() {
        this.running = false;
        if (!multicastSocket.isClosed()) {
            multicastSocket.close();
        }
    }

    @Override
    public void run() {
        DatagramPacket datagramPacket;
        HeartBeatMsg msg;
        ObjectInputStream in = null;

        if (multicastSocket == null || controller == null) return;

        try {
            System.out.println("[HeartBeatThread] Running HeartBeatThread");
            controller.addToHeartBeatConsole("[HeartBeatThread] Running HeartBeatThread");
            multicastSocket.setSoTimeout(Constants.HEARTBEAT_BACKUP_TIMEOUT * 1000);

            while (running) {
                datagramPacket = new DatagramPacket(new byte[Constants.HEARTBEAT_MAX_SIZE], Constants.HEARTBEAT_MAX_SIZE);
                multicastSocket.receive(datagramPacket);

                try {
                    in = new ObjectInputStream(new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength()));
                    Object returnedObject = in.readObject();

                    if (returnedObject instanceof HeartBeatMsg) {
                        msg = (HeartBeatMsg) returnedObject;
                        serverBackup.setHearBeatMsgReceived(msg);
                        System.out.println("[HeartBeatThread] Msg de " + datagramPacket.getAddress() + ":" + datagramPacket.getPort());
                        controller.addToHeartBeatConsole("[HeartBeatThread] Msg de " + datagramPacket.getAddress() + ":" + datagramPacket.getPort());
                    }

                } catch (ClassNotFoundException e) {
                    System.out.println("[HeartBeatThread] Msg format unexpected");
                    controller.addToHeartBeatConsole("[HeartBeatThread] Msg format unexpected");
                } catch (IOException e) {
                    System.out.println("[HeartBeatThread] Can't read Msg");
                    controller.addToHeartBeatConsole("[HeartBeatThread] Can't read Msg");
                }
            }

            if (in != null) {
                in.close();
            }

        } catch (SocketTimeoutException e) {
            System.out.println("[HeartBeatThread] Timeout - No messages received for 30 seconds. Terminating...");
            controller.addToHeartBeatConsole("[HeartBeatThread] Timeout - No messages received for 30 seconds. Terminating...");
            stopHeartBeatThread();
            Platform.exit();
        } catch (IOException e) {
            stopHeartBeatThread();
        }
    }
}
