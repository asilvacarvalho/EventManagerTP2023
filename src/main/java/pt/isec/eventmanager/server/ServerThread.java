package pt.isec.eventmanager.server;

import pt.isec.eventmanager.db.CodigoRegistoModel;
import pt.isec.eventmanager.db.EventManagerDB;
import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.users.User;
import pt.isec.eventmanager.users.UserKey;
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
    //private int threadNumber;
    private final String dbUrl;

    public ServerThread(Socket toClientSocket, int threadNumber, String dbUrl, ServerController controller) {
        this.serverController = controller;
        this.toClientSocket = toClientSocket;
        //this.threadNumber = threadNumber;
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
                        case Constants.EDITUSER_REQUEST:
                            editUser(oin, oout, dbUrl);
                            break;
                        case Constants.INSERTUSERKEY_REQUEST:
                            insertUserKey(oin, oout, dbUrl);
                            break;
                        case Constants.LISTUSEREVENTS_REQUEST:
                            listUserEvents(oin, oout, dbUrl);
                            break;
                        case Constants.INSERTEVENT_REQUEST:
                            insertEvent(oin, oout, dbUrl);
                            break;
                        case Constants.LISTEVENTS_REQUEST:
                            listEvents(oout, dbUrl);
                            break;
                        case Constants.EDITEVENT_REQUEST:
                            editEvent(oin, oout, dbUrl);
                            break;
                        case Constants.DELETEEVENT_REQUEST:
                            deleteEvent(oin, oout, dbUrl);
                            break;
                        case Constants.EVENTHASATTENDENCES_REQUEST:
                            eventHasAttendences(oin, oout, dbUrl);
                            break;
                        case Constants.GETEVENTKEY_REQUEST:
                            getEventKey(oin, oout, dbUrl);
                            break;
                        case Constants.GENERATEEVENTKEY_REQUEST:
                            insertEventKey(oin, oout, dbUrl);
                            break;
                        case Constants.LISTATTENDENCES_REQUEST:
                            listAttendances(oin, oout, dbUrl);
                            break;
                        case Constants.ADDATTENDENCE_REQUEST:
                            insertAttendance(oin, oout, dbUrl);
                            break;
                        case Constants.DELETEATTENDENCE_REQUEST:
                            deleteAttendance(oin, oout, dbUrl);
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

    //USERS
    private void authenticateUser(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        User requestUser = (User) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            User authenticatedUser = EventManagerDB.authenticateUser(conn, requestUser, serverController);

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

            boolean registationSuccess = EventManagerDB.insertUser(conn, newUser, serverController);

            oout.writeObject(registationSuccess);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void editUser(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        User newUser = (User) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            boolean success = EventManagerDB.editUser(conn, newUser, serverController);

            oout.writeObject(success);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void listUserEvents(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        String username = (String) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            ArrayList<Event> listEvents = EventManagerDB.listUserEvents(conn, username, serverController);

            oout.writeObject(listEvents);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    //EVENTS
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

    private void editEvent(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        Event newEvent = (Event) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            boolean editEventSuccess = EventManagerDB.editEvent(conn, newEvent, serverController);

            oout.writeObject(editEventSuccess);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void deleteEvent(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        Event newEvent = (Event) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            boolean success = EventManagerDB.deleteEvent(conn, newEvent, serverController);

            oout.writeObject(success);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void eventHasAttendences(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        int eventId = (int) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            boolean eventHasAttendences = EventManagerDB.eventHasAttendences(conn, eventId, serverController);

            oout.writeObject(eventHasAttendences);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void listAttendances(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        int eventId = (int) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            ArrayList<Attendance> listEvents = EventManagerDB.listAttendancesForEvent(conn, eventId, serverController);

            oout.writeObject(listEvents);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void insertAttendance(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        Attendance newAttendance = (Attendance) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            boolean success = EventManagerDB.insertAttendanceEvent(conn, newAttendance.getEventId(), newAttendance.getUsername(), serverController);

            oout.writeObject(success);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void deleteAttendance(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        Attendance newAttendance = (Attendance) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            boolean success = EventManagerDB.deletePresenceFromEvent(conn, newAttendance.getEventId(), newAttendance.getUsername(), serverController);

            oout.writeObject(success);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    //EVENT KEY
    private void getEventKey(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        int eventId = (int) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            EventKey eventKey = EventManagerDB.getEventKey(conn, eventId, serverController);

            oout.writeObject(eventKey);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    private void insertEventKey(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        EventKey eventKey = (EventKey) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            boolean insertSuccess = EventManagerDB.insertEventKey(conn, eventKey, serverController);

            oout.writeObject(insertSuccess);
            oout.flush();
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    //USER KEY
    private void insertUserKey(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        UserKey userKey = (UserKey) oin.readObject();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            int eventId = CodigoRegistoModel.getEventId(conn, userKey.getUserKey(), serverController);

            if (eventId < 0) {
                System.err.println("[ServerThread] Insert User Key Error: Invalid Key");
                serverController.addToConsole("[ServerThread] Insert User Key Error: Invalid Key");
                oout.writeObject(false);
                oout.flush();
            } else {
                boolean success = EventManagerDB.insertPresenceForEvent(conn, eventId, userKey.getUsername(), serverController);

                oout.writeObject(success);
                oout.flush();
            }
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }
}
