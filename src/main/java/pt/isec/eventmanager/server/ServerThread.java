package pt.isec.eventmanager.server;

import pt.isec.eventmanager.db.CodigoRegistoModel;
import pt.isec.eventmanager.db.EventManagerDB;
import pt.isec.eventmanager.db.EventoUtilizadorModel;
import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.rmi.ServerService;
import pt.isec.eventmanager.users.User;
import pt.isec.eventmanager.users.UserKey;
import pt.isec.eventmanager.util.Constants;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class ServerThread extends Thread {
    private final ServerController serverController;
    private final Socket toClientSocket;
    private final String dbUrl;

    private ObjectOutputStream oout;
    private ObjectInputStream oin;

    private Server server;
    private ServerService serverService;

    private final Object lock = new Object();

    private volatile boolean isServerRunning;

    private boolean isContinuosCommunication;

    public ServerThread(Socket toClientSocket, String dbUrl, ServerController controller, Server server, ServerService serverService) {
        this.serverController = controller;
        this.toClientSocket = toClientSocket;
        this.dbUrl = dbUrl;
        this.server = server;
        this.serverService = serverService;
        this.isServerRunning = true;
        this.isContinuosCommunication = false;

        System.out.println("[ServerThread] Thread id " + this.getId() + " Running.");
        serverController.addToConsole("[ServerThread] Thread id " + this.getId() + " Running.");
    }

    public boolean isContinuosCommunication() {
        return isContinuosCommunication;
    }

    public void stopServerThread() {
        isServerRunning = false;

        System.out.println("[ServerThread] id " + this.getId() + " Stoped.");
        serverController.addToConsole("[ServerThread] id " + this.getId() + " Stoped.");
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream oout = new ObjectOutputStream(toClientSocket.getOutputStream());
            ObjectInputStream oin = new ObjectInputStream(toClientSocket.getInputStream());

            this.oout = oout;
            this.oin = oin;

            try {
                String messageType = (String) oin.readObject();

                if (messageType.equals("CONTINUOUS_COMMUNICATION"))
                    isContinuosCommunication = true;
            } catch (ClassNotFoundException e) {
                stopServerThread();
            }

            while (isServerRunning) {
                try {
                    ServerOperation operation = (ServerOperation) oin.readObject();

                    switch (operation.getOperation()) {
                        case Constants.AUTHENTICATION_REQUEST:
                            authenticateUser(operation);
                            break;
                        case Constants.INSERTUSER_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            insertUser(operation);
                            break;
                        case Constants.EDITUSER_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            editUser(operation);
                            break;
                        case Constants.INSERTUSERKEY_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            insertUserKey(operation);
                            break;
                        case Constants.LISTUSEREVENTS_REQUEST:
                            listUserEvents(operation);
                            break;
                        case Constants.INSERTEVENT_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            insertEvent(operation);
                            break;
                        case Constants.LISTEVENTS_REQUEST:
                            listEvents(operation);
                            break;
                        case Constants.EDITEVENT_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            editEvent(operation);
                            break;
                        case Constants.DELETEEVENT_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            deleteEvent(operation);
                            break;
                        case Constants.EVENTHASATTENDENCES_REQUEST:
                            eventHasAttendences(operation);
                            break;
                        case Constants.GETEVENTKEY_REQUEST:
                            getEventKey(operation);
                            break;
                        case Constants.INSERTEVENTKEY_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            insertEventKey(operation);
                            break;
                        case Constants.LISTATTENDENCES_REQUEST:
                            listAttendances(operation);
                            break;
                        case Constants.ADDATTENDENCE_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            insertAttendance(operation);
                            break;
                        case Constants.DELETEATTENDENCE_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            deleteAttendance(operation);
                            break;
                        default:
                            System.out.println("[ServerThread] Unsupported Operation");
                            serverController.addToConsole("[ServerThread] Unsupported Operation");
                            break;
                    }

                } catch (EOFException e) {
                    System.out.println("[ServerThread] Client Disconnected.");
                    serverController.addToConsole("[ServerThread] Client Disconnected.");
                    System.out.println("[ServerThread] id " + this.getId() + " Stoped.");
                    serverController.addToConsole("[ServerThread] id " + this.getId() + " Stoped.");
                    server.getServerThreadsList().remove(this);
                    break;
                } catch (Exception e) {
                    System.out.println("[ServerThread] Communication error: " + e.getMessage());
                    serverController.addToConsole("[ServerThread] Communication error: " + e.getMessage());
                    System.out.println("[ServerThread] id " + this.getId() + " Stoped.");
                    serverController.addToConsole("[ServerThread] id " + this.getId() + " Stoped.");
                    server.getServerThreadsList().remove(this);
                    break;
                }
            }

            oout.close();
            oin.close();
            toClientSocket.close();

        } catch (IOException e) {
            System.out.println("[ServerThread] Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Error: " + e.getMessage());
        }
    }

    //USERS
    private void authenticateUser(ServerOperation operation) throws SQLException, IOException {
        User requestUser = operation.getUser();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Authenticating User");
        serverController.addToConsole("[ServerThread] Authenticating User");

        User authenticatedUser = EventManagerDB.authenticateUser(conn, requestUser);

        operation.setUser(authenticatedUser);

        oout.writeObject(operation);
        oout.flush();

        conn.close();
    }

    private void insertUser(ServerOperation operation) throws SQLException, IOException {
        User newUser = operation.getUser();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Inserting User");
        serverController.addToConsole("[ServerThread] Inserting User");

        User existingUser = EventManagerDB.getUser(conn, newUser.getEmail());

        if (existingUser != null) {
            System.err.println("[ServerThread] Error inserting user, email already exists");
            serverController.addToConsole("[ServerThread] Error inserting user, email already exists");
            operation.setResult(false);
            oout.writeObject(operation);
            oout.flush();
            conn.close();
            return;
        }

        synchronized (lock) {
            boolean success = EventManagerDB.insertUser(conn, newUser);
            operation.setResult(success);
            oout.writeObject(operation);
            oout.flush();

            if (success)
                serverService.inserUser(server.getDbVersion(), newUser);

            incrementDBVersion(success, conn);
        }

        conn.close();
    }

    private void editUser(ServerOperation operation) throws SQLException, IOException {
        User newUser = operation.getUser();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Editing User");
        serverController.addToConsole("[ServerThread] Editing User");

        User existingUser = EventManagerDB.getUser(conn, newUser.getEmail());

        if (existingUser == null) {
            System.err.println("[ServerThread] Error updating user, email doesn't exists");
            serverController.addToConsole("[ServerThread] Error updating user, email doesn't exists");
            operation.setResult(false);
            oout.writeObject(operation);
            oout.flush();
            conn.close();
            return;
        }

        synchronized (lock) {
            boolean success = EventManagerDB.editUser(conn, newUser);
            operation.setResult(success);
            oout.writeObject(operation);
            oout.flush();

            if (success)
                serverService.editUser(server.getDbVersion(), newUser);

            incrementDBVersion(success, conn);
        }

        conn.close();
    }

    private void listUserEvents(ServerOperation operation) throws SQLException, IOException {
        String username = operation.getUserName();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Listing User Events");
        serverController.addToConsole("[ServerThread] Listing User Events");

        ArrayList<Integer> eventIds = EventoUtilizadorModel.getEventIdsForUser(conn, username);

        ArrayList<Event> listEvents = new ArrayList<>();

        if (!eventIds.isEmpty()) {
            listEvents = EventManagerDB.listUserEvents(conn, eventIds);
        }

        operation.setListEvents(listEvents);
        oout.writeObject(operation);
        oout.flush();

        conn.close();
    }

    //EVENTS
    private void listEvents(ServerOperation operation) throws SQLException, IOException {

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Listing Events");
        serverController.addToConsole("[ServerThread] Listing Events");

        ArrayList<Event> listEvents = EventManagerDB.listEvents(conn);

        operation.setListEvents(listEvents);
        oout.writeObject(operation);
        oout.flush();

        conn.close();
    }

    private void insertEvent(ServerOperation operation) throws SQLException, IOException {
        Event newEvent = operation.getEvent();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Inserting Event");
        serverController.addToConsole("[ServerThread] Inserting Event");

        synchronized (lock) {
            boolean success = EventManagerDB.insertEvent(conn, newEvent);

            operation.setResult(success);
            oout.writeObject(operation);
            oout.flush();

            if (success) {
                serverService.inserEvent(server.getDbVersion(), newEvent);
                server.refreshClientEvents();
            }

            incrementDBVersion(success, conn);
        }

        conn.close();
    }

    private void editEvent(ServerOperation operation) throws SQLException, IOException {
        Event newEvent = operation.getEvent();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Editing Event");
        serverController.addToConsole("[ServerThread] Editing Event");

        Event existingEvent = EventManagerDB.getEvent(conn, newEvent.getId());

        if (existingEvent == null) {
            System.err.println("[ServerThread] Event with ID " + newEvent.getId() + " not found.");
            serverController.addToConsole("[ServerThread] Event with ID " + newEvent.getId() + " not found.");
            operation.setResult(false);
            oout.writeObject(operation);
            oout.flush();
            conn.close();
            return;
        }

        synchronized (lock) {
            boolean success = EventManagerDB.editEvent(conn, newEvent);

            operation.setResult(success);
            oout.writeObject(operation);
            oout.flush();

            if (success) {
                serverService.editEvent(server.getDbVersion(), newEvent);
                server.refreshClientEvents();
            }

            incrementDBVersion(success, conn);
        }

        conn.close();
    }

    private void deleteEvent(ServerOperation operation) throws SQLException, IOException {
        Event newEvent = operation.getEvent();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Deleting Event");
        serverController.addToConsole("[ServerThread] Deleting Event");

        if (!EventManagerDB.eventHasAttendences(conn, newEvent.getId())) {
            synchronized (lock) {
                boolean success = EventManagerDB.deleteEvent(conn, newEvent);

                operation.setResult(success);
                oout.writeObject(operation);
                oout.flush();
                if (success) {
                    serverService.deleteEvent(server.getDbVersion(), newEvent);
                    server.refreshClientEvents();
                }

                incrementDBVersion(success, conn);
            }
        } else {
            System.err.println("[ServerThread] Event cannot be deleted because it has attendees.");
            serverController.addToConsole("[ServerThread] Event cannot be deleted because it has attendees.");

            operation.setResult(false);
            oout.writeObject(operation);
            oout.flush();
        }

        conn.close();
    }

    private void eventHasAttendences(ServerOperation operation) throws SQLException, IOException {
        int eventId = operation.getEventId();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Checking Event Attendances");
        serverController.addToConsole("[ServerThread] Checking Event Attendances");

        boolean success = EventManagerDB.eventHasAttendences(conn, eventId);

        operation.setResult(success);
        oout.writeObject(operation);
        oout.flush();

        conn.close();
    }

    private void listAttendances(ServerOperation operation) throws SQLException, IOException {
        int eventId = operation.getEventId();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Listing Attendances for Event");
        serverController.addToConsole("[ServerThread] Listing Attendances for Event");

        ArrayList<Attendance> listAttendances = EventManagerDB.listAttendancesForEvent(conn, eventId);

        operation.setListAttendances(listAttendances);
        oout.writeObject(operation);
        oout.flush();
    }

    private void insertAttendance(ServerOperation operation) throws SQLException, IOException {
        Attendance newAttendance = operation.getAttendance();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Inserting Attendance");
        serverController.addToConsole("[ServerThread] Inserting Attendance");

        synchronized (lock) {
            boolean success = EventManagerDB.insertAttendanceEvent(conn, newAttendance.getEventId(), newAttendance.getUsername());

            operation.setResult(success);
            oout.writeObject(operation);
            oout.flush();

            if (success) {
                serverService.inserAttendance(server.getDbVersion(), newAttendance.getEventId(), newAttendance.getUsername());
                server.refreshClientAttendances(newAttendance.getEventId());

                sendEmail(newAttendance.getUsername(), "Attendence Inserted", "Attendence for " + newAttendance.getEventId() + " Inserted");
            }

            incrementDBVersion(success, conn);
        }

        conn.close();
    }

    private void deleteAttendance(ServerOperation operation) throws SQLException, IOException {
        Attendance newAttendance = operation.getAttendance();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Deleting Attendance");
        serverController.addToConsole("[ServerThread] Deleting Attendance");

        synchronized (lock) {
            boolean success = EventManagerDB.deletePresenceFromEvent(conn, newAttendance.getEventId(), newAttendance.getUsername());

            operation.setResult(success);
            oout.writeObject(operation);
            oout.flush();

            if (success) {
                serverService.deleteAttendance(server.getDbVersion(), newAttendance.getEventId(), newAttendance.getUsername());
                server.refreshClientAttendances(newAttendance.getEventId());

                sendEmail(newAttendance.getUsername(), "Attendence Deleted", "Attendence from " + newAttendance.getEventId() + " Deleted");
            }

            incrementDBVersion(success, conn);
        }

        conn.close();
    }

    //EVENT KEY
    private void getEventKey(ServerOperation operation) throws SQLException, IOException {
        int eventId = operation.getEventId();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Getting Event Key");
        serverController.addToConsole("[ServerThread] Getting Event Key");

        EventKey eventKey = EventManagerDB.getEventKey(conn, eventId);

        operation.setEventKey(eventKey);
        oout.writeObject(operation);
        oout.flush();

        conn.close();
    }

    private void insertEventKey(ServerOperation operation) throws SQLException, IOException {
        EventKey eventKey = operation.getEventKey();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Inserting Event Key");
        serverController.addToConsole("[ServerThread] Inserting Event Key");

        EventKey existingEventKey = EventManagerDB.getEventKey(conn, eventKey.getEventId());

        synchronized (lock) {
            boolean success;

            if (existingEventKey != null) {
                success = EventManagerDB.deleteEventKey(conn, existingEventKey) &&
                        EventManagerDB.insertEventKey(conn, eventKey);
                if (success) {
                    serverService.deleteEventKey(server.getDbVersion(), existingEventKey);
                    serverService.inserEventKey(server.getDbVersion(), eventKey);
                }
            } else {
                success = EventManagerDB.insertEventKey(conn, eventKey);
                if (success) {
                    serverService.inserEventKey(server.getDbVersion(), eventKey);
                }
            }

            operation.setResult(success);
            oout.writeObject(operation);
            oout.flush();

            incrementDBVersion(success, conn);
        }

        conn.close();
    }

    //USER KEY
    private void insertUserKey(ServerOperation operation) throws SQLException, IOException {
        UserKey userKey = operation.getUserKey();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);

        System.out.println("[ServerThread] Inserting User Attendance");
        serverController.addToConsole("[ServerThread] Inserting User Attendance");

        int eventId = EventManagerDB.getEventId(conn, userKey.getUserKey());

        if (eventId < 0) {
            System.err.println("[ServerThread] Insert User Key Error: Invalid Key");
            serverController.addToConsole("[ServerThread] Insert User Key Error: Invalid Key");
            operation.setResult(false);
            oout.writeObject(operation);
            oout.flush();
            conn.close();
            return;
        }

        // Verificar se a data atual está entre start_date e end_date do evento
        Date currentDate = new Date();
        Event event = EventManagerDB.getEvent(conn, eventId);
        Date eventDate = event.getDate();
        String startTime = event.getStartTime();
        String endTime = event.getEndTime();

        Calendar startDate = fillEventDateTime(eventDate, startTime);
        Calendar endDate = fillEventDateTime(eventDate, endTime);

        if (startDate == null || endDate == null) {
            System.err.println("[ServerThread] Insert User Key Error");
            serverController.addToConsole("[ServerThread] Insert User Key Error");
            operation.setResult(false);
            oout.writeObject(operation);
            oout.flush();
            conn.close();
            return;
        }

        if (currentDate.after(startDate.getTime()) && currentDate.before(endDate.getTime())) {
            EventKey eventKey = CodigoRegistoModel.getEventKey(conn, eventId);
            if (eventKey == null) {
                System.err.println("[ServerThread] Insert User Key Error");
                serverController.addToConsole("[ServerThread] Insert User Key Error");
                operation.setResult(false);
                oout.writeObject(operation);
                oout.flush();
                conn.close();
                return;
            }
            Date eventKeyEndDate = eventKey.getEndDate();

            if (currentDate.before(eventKeyEndDate)) {
                synchronized (lock) {
                    boolean success = EventManagerDB.insertPresenceForEvent(conn, eventId, userKey.getUsername());

                    operation.setResult(success);
                    oout.writeObject(operation);
                    oout.flush();

                    if (success) {
                        serverService.inserAttendance(server.getDbVersion(), eventId, userKey.getUsername());
                        //server.refreshClientAttendances();

                        sendEmail(userKey.getUsername(), "Attendence Inserted", "Attendence for " + eventId + " Inserted");
                    }

                    incrementDBVersion(success, conn);


                }
            } else {
                System.err.println("[ServerThread] Insert User Key Error: Current date is after the event key end date");
                serverController.addToConsole("[ServerThread] Insert User Key Error: Current date is after the event key end date");
                operation.setResult(false);
                oout.writeObject(operation);
                oout.flush();
            }
        } else {
            System.err.println("[ServerThread] Insert User Key Error: Current date is not within the event date range");
            serverController.addToConsole("[ServerThread] Insert User Key Error: Current date is not within the event date range");
            operation.setResult(false);
            oout.writeObject(operation);
            oout.flush();
        }

        conn.close();
    }

    private Calendar fillEventDateTime(Date eventDate, String time) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        Date startDateTime;
        try {
            startDateTime = timeFormat.parse(time);
        } catch (ParseException e) {
            System.err.println("[ServerThread] Error Filling Event Date: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Error Filling Event Date: " + e.getMessage());
            return null;
        }

        Calendar startTimeC = Calendar.getInstance();
        startTimeC.setTime(startDateTime);

        Calendar eventDateTime = Calendar.getInstance();
        eventDateTime.setTime(eventDate);
        eventDateTime.set(Calendar.HOUR_OF_DAY, startTimeC.get(Calendar.HOUR_OF_DAY));
        eventDateTime.set(Calendar.MINUTE, startTimeC.get(Calendar.MINUTE));
        eventDateTime.set(Calendar.SECOND, 0);

        return eventDateTime;
    }

    //EMAIL - TODO: o google não deixa a conta ter acesso a ligações menos seguras...
    private void sendEmail(String recipientEmail, String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Criação de uma sessão de e-mail com autenticação
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Constants.ADMIN_EMAIL, Constants.ADMIN_EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(Constants.ADMIN_EMAIL)); // Remetente
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail)); // Destinatário
            message.setSubject(subject); // Assunto do e-mail
            message.setText(body); // Corpo do e-mail

            Transport.send(message); // Envio do e-mail

            System.out.println("[ServerThread] E-mail enviado com sucesso para: " + recipientEmail);
            serverController.addToConsole("[ServerThread] E-mail enviado com sucesso para: " + recipientEmail);
        } catch (MessagingException e) {
            System.out.println("[ServerThread] Erro ao enviar e-mail: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Erro ao enviar e-mail: " + e.getMessage());
        }
    }

    //DB
    private void incrementDBVersion(boolean increment, Connection conn) throws SQLException {
        if (!increment) return;

        int newDBVersion = EventManagerDB.incrementDBVersion(conn);

        if (newDBVersion > 0) {
            System.out.println("[ServerThread] DBVersion Increment Success");
            serverController.addToConsole("[ServerThread] DBVersion Increment Success");
            server.setDbVersion(newDBVersion);
        } else {
            System.out.println("[ServerThread] DBVersion Increment Error");
            serverController.addToConsole("[ServerThread] DBVersion Increment Error");
            server.stopServer();
        }
    }

    private boolean isBDFileCopyOngoing() throws InterruptedException {
        int tries = 0;

        while (server.serverServiceGetDBFileIsRunning() && tries < 5) {
            Thread.sleep(5000);
            tries++;
        }

        return tries > 5;
    }

    public void refreshClientEvents() throws IOException {
        ServerOperation serverOperation = new ServerOperation(Constants.REFRESH_EVENTS);

        oout.writeObject(serverOperation);
        oout.flush();
    }

    public void refreshClientAttendances(int eventId) throws IOException {
        ServerOperation serverOperation = new ServerOperation(Constants.REFRESH_ATTENDANCES);
        serverOperation.setEventId(eventId);

        oout.writeObject(serverOperation);
        oout.flush();
    }
}
