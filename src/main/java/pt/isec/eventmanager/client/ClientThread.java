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

    private volatile boolean isClientRunning;

    public ClientThread(Socket toServerSocket, Client client) {
        this.client = client;
        this.toServerSocket = toServerSocket;
    }

    public void stopServerThread() {
        isClientRunning = false;
        System.out.println("[ClientThread] id " + this.getId() + " Stoped.");
    }

    @Override
    public void run() {
        isClientRunning = true;
        System.out.println("[ClientThread] id " + this.getId() + " Running.");

        try {
            ObjectOutputStream oout = new ObjectOutputStream(toServerSocket.getOutputStream());
            ObjectInputStream oin = new ObjectInputStream(toServerSocket.getInputStream());

            while (isClientRunning) {
                try {
                    // Ler o tipo de operação do cliente
                    if (oin.readObject() instanceof ServerOperation) {

                        ServerOperation operation = (ServerOperation) oin.readObject();

                        String operationType = operation.getOperation();
                        System.out.println("OPERATION: " + operationType);

                        // Lidar com base no tipo de operação
                        switch (operationType) {
                            case Constants.REFRESH_EVENTS:
                                refreshEvents(oin);
                                break;
                            case Constants.REFRESH_ATTENDANCES:
                                refreshAttendances(oin);
                                break;
                            default:
                                System.out.println("[ClientThread] Unsupported Operation: " + operationType);
                                break;
                        }
                    }
                } catch (EOFException e) {
                    System.out.println("[ClientThread] Client Disconnected.");
                    System.out.println("[ClientThread] id " + this.getId() + " Stoped.");
                    //server.getServerThreadsList().remove(this);
                } catch (Exception e) {
                    System.out.println("[ClientThread] Communication error: " + e.getMessage());
                    //server.getServerThreadsList().remove(this);
                }
            }
            oin.close();
        } catch (IOException e) {
            System.out.println("[ClientThread] Error creating input/output streams: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshEvents(ObjectInputStream oin) throws IOException, ClassNotFoundException {
//        ArrayList<Event> listEvents = (ArrayList<Event>) oin.readObject();
//
//        System.out.println("FODASSSSEEE" + listEvents.size());
//        client.getClientAuthenticatedController().refreshListEvens(listEvents);
    }

    private void refreshAttendances(ObjectInputStream oin) {

    }
}
