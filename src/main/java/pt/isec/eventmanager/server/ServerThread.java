package pt.isec.eventmanager.server;

import pt.isec.eventmanager.db.EventManagerDB;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.users.User;
import pt.isec.eventmanager.util.Constants;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

public class ServerThread extends Thread {
    private final ServerController serverController;
    private final Socket toClientSocket;
    private int threadNumber;
    private final String dbUrl;

    public ServerThread(Socket toClientSocket, int threadNumber, String dbUrl, ServerController controller) {
        this.serverController = controller;
        this.toClientSocket = toClientSocket;
        this.threadNumber = threadNumber;
        this.dbUrl = dbUrl;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream oout = new ObjectOutputStream(toClientSocket.getOutputStream());
            ObjectInputStream oin = new ObjectInputStream(toClientSocket.getInputStream());

            while (true) {
                try {
                    // Ler o tipo de operação do cliente
                    String operationType = (String) oin.readObject();

                    // Lidar com base no tipo de operação
                    switch (operationType) {
                        case Constants.AUTHENTICATION_REQUEST:
                            authenticateUser(oin, oout, dbUrl);
                            break;
                        case Constants.INSERTUSER_REQUEST:
                            insertUser(oin, oout, dbUrl);
                            break;
                        case Constants.INSERTEVENT_REQUEST:
                            insertEvent(oin, oout, dbUrl);
                            break;
                        case Constants.LISTEVENTS_REQUEST:
                            listEvents(oout, dbUrl);
                            break;
                        default:
                            System.out.println("[ServerThread] Unsupported Operation: " + operationType);
                            serverController.addToConsole("[ServerThread] Unsupported Operation: " + operationType);
                            break;
                    }

                } catch (EOFException e) {
                    System.out.println("[ServerThread] Client Disconnected.");
                    serverController.addToConsole("[ServerThread] Client Disconnected.");
                    break;
                } catch (Exception e) {
                    System.out.println("[ServerThread] Client communication error: " + e.getMessage());
                    serverController.addToConsole("[ServerThread] Client communication error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("[ServerThread] Error creating input/output streams: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Error creating input/output streams: " + e.getMessage());
        }
    }

    private void authenticateUser(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        User requestUser = (User) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            User authenticatedUser = EventManagerDB.authenticateUser(conn, requestUser);

            oout.writeObject(authenticatedUser);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void insertUser(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        User newUser = (User) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            boolean registationSuccess = EventManagerDB.insertUser(conn, newUser);

            oout.writeObject(registationSuccess);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void insertEvent(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        Event newEvent = (Event) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            boolean eventAddedSuccess = EventManagerDB.insertEvent(conn, newEvent, serverController);

            oout.writeObject(eventAddedSuccess);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void listEvents(ObjectOutputStream oout, String dbUrl) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            ArrayList<Event> listEvents = EventManagerDB.listEvents(conn, serverController);

            oout.writeObject(listEvents);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }
}
