package pt.isec.eventmanager.client;

import pt.isec.eventmanager.server.ServerOperation;
import pt.isec.eventmanager.util.Constants;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientThread extends Thread {
    private Client client;
    private Socket toServerSocket;

    ObjectInputStream oin;
    ObjectOutputStream oout;

    private volatile boolean isClientRunning;

    public ClientThread(Socket toServerSocket, ObjectInputStream oin, ObjectOutputStream oout, Client client) {
        this.client = client;
        this.toServerSocket = toServerSocket;
        this.oin = oin;
        this.oout = oout;
    }

    public void stopClientThread() {
        isClientRunning = false;
        System.out.println("[ClientThread] ClientThread id " + this.getId() + " Stoped.");
    }

    @Override
    public void run() {
        isClientRunning = true;
        System.out.println("[ClientThread] ClientThread id " + this.getId() + " Running.");

        try {
            oout.writeObject("CONTINUOUS_COMMUNICATION");
            oout.flush();
        } catch (IOException e) {
            stopClientThread();
        }

        while (isClientRunning) {
            try {
                ServerOperation operation = (ServerOperation) oin.readObject();

                switch (operation.getOperation()) {
                    case Constants.REFRESH_EVENTS:
                        refreshEvents();
                        break;
                    case Constants.REFRESH_ATTENDANCES:
                        refreshAttendances(operation);
                        break;
                    default:
                        System.out.println("[ClientThread] Unsupported Operation");
                        break;
                }

            } catch (EOFException e) {
                System.out.println("[ClientThread] Client Disconnected.");
                System.out.println("[ClientThread] id " + this.getId() + " Stoped.");
                break;
            } catch (Exception e) {
                System.out.println("[ClientThread] Communication error: " + e.getMessage());
                break;
            }
        }
    }

    private void refreshEvents() {
        System.out.println("[ClientThread] Refreshing Events...");
        client.refreshClientEvents();
    }

    private void refreshAttendances(ServerOperation operation) {
        System.out.println("[ClientThread] Refreshing Attendances...");
        client.refreshClientAttendances(operation.getEventId());
    }
}
