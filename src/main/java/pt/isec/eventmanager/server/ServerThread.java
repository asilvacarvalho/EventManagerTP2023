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
    private final int id;

    private final ServerController serverController;
    private final Socket toClientSocket;
    private final String dbUrl;

    private Server server;
    private ServerService serverService;

    private final Object lock = new Object();

    private volatile boolean isServerRunning;

    public ServerThread(Socket toClientSocket, int threadNumber, String dbUrl, ServerController controller, Server server, ServerService serverService) {
        this.serverController = controller;
        this.toClientSocket = toClientSocket;
        this.dbUrl = dbUrl;
        this.server = server;
        this.serverService = serverService;
        this.isServerRunning = true;

        this.id = threadNumber;

        System.out.println("[ServerThread] Thread id " + this.getId() + " Running.");
        serverController.addToConsole("[ServerThread] Thread id " + this.getId() + " Running.");
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

            while (isServerRunning) {
                try {
                    // Ler o tipo de operação do cliente
                    String operationType = (String) oin.readObject();

                    // Lidar com base no tipo de operação
                    switch (operationType) {
                        case Constants.AUTHENTICATION_REQUEST:
                            authenticateUser(oin, oout, dbUrl);
                            break;
                        case Constants.INSERTUSER_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            insertUser(oin, oout, dbUrl);
                            break;
                        case Constants.EDITUSER_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            editUser(oin, oout, dbUrl);
                            break;
                        case Constants.INSERTUSERKEY_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            insertUserKey(oin, oout, dbUrl);
                            break;
                        case Constants.LISTUSEREVENTS_REQUEST:
                            listUserEvents(oin, oout, dbUrl);
                            break;
                        case Constants.INSERTEVENT_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            insertEvent(oin, oout, dbUrl);
                            break;
                        case Constants.LISTEVENTS_REQUEST:
                            listEvents(oout, dbUrl);
                            break;
                        case Constants.EDITEVENT_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            editEvent(oin, oout, dbUrl);
                            break;
                        case Constants.DELETEEVENT_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            deleteEvent(oin, oout, dbUrl);
                            break;
                        case Constants.EVENTHASATTENDENCES_REQUEST:
                            eventHasAttendences(oin, oout, dbUrl);
                            break;
                        case Constants.GETEVENTKEY_REQUEST:
                            getEventKey(oin, oout, dbUrl);
                            break;
                        case Constants.GENERATEEVENTKEY_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            insertEventKey(oin, oout, dbUrl);
                            break;
                        case Constants.LISTATTENDENCES_REQUEST:
                            listAttendances(oin, oout, dbUrl);
                            break;
                        case Constants.ADDATTENDENCE_REQUEST:
                            if (isBDFileCopyOngoing()) return;
                            insertAttendance(oin, oout, dbUrl);
                            break;
                        case Constants.DELETEATTENDENCE_REQUEST:
                            if (isBDFileCopyOngoing()) return;
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
                    server.getServerThreadsList().remove(this);
                    break;
                } catch (Exception e) {
                    System.out.println("[ServerThread] Communication error: " + e.getMessage());
                    serverController.addToConsole("[ServerThread] Communication error: " + e.getMessage());
                    server.getServerThreadsList().remove(this);
                    break;
                }
            }
            oout.close();
            oin.close();
        } catch (IOException e) {
            System.out.println("[ServerThread] Error creating input/output streams: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Error creating input/output streams: " + e.getMessage());
        }
    }

    //USERS
    private void authenticateUser(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        User requestUser = (User) oin.readObject();

        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        User authenticatedUser = EventManagerDB.authenticateUser(conn, requestUser);

        oout.writeObject(authenticatedUser);
        oout.flush();
    }

    private void insertUser(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        User newUser = (User) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        User existingUser = EventManagerDB.getUser(conn, newUser.getEmail());

        if (existingUser != null) {
            System.err.println("[EventManagerDB] Error inserting user, email already exists");
            serverController.addToConsole("[EventManagerDB] Error inserting user, email already exists");
            oout.writeObject(false);
            oout.flush();
            return;
        }

        synchronized (lock) {
            boolean success = EventManagerDB.insertUser(conn, newUser);
            oout.writeObject(success);
            oout.flush();

            if (success)
                serverService.inserUser(server.getDbVersion(), newUser);

            incrementDBVersion(success);
        }
    }

    private void editUser(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        User newUser = (User) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        User existingUser = EventManagerDB.getUser(conn, newUser.getEmail());

        if (existingUser == null) {
            System.err.println("[EventManagerDB] Error updating user, email doesn't exists");
            serverController.addToConsole("[EventManagerDB] Error updating user, email doesn't exists");
            oout.writeObject(false);
            oout.flush();
            return;
        }

        synchronized (lock) {
            boolean success = EventManagerDB.editUser(conn, newUser);
            oout.writeObject(success);
            oout.flush();

            incrementDBVersion(success);
        }
    }

    private void listUserEvents(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        String username = (String) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        ArrayList<Integer> eventIds = EventoUtilizadorModel.getEventIdsForUser(conn, username);

        ArrayList<Event> listEvents = new ArrayList<>();

        if (!eventIds.isEmpty()) {
            listEvents = EventManagerDB.listUserEvents(conn, eventIds);
        }

        oout.writeObject(listEvents);
        oout.flush();
    }

    //EVENTS
    private void listEvents(ObjectOutputStream oout, String dbUrl) throws SQLException, IOException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        ArrayList<Event> listEvents = EventManagerDB.listEvents(conn);

        oout.writeObject(listEvents);
        oout.flush();
    }

    private void insertEvent(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        Event newEvent = (Event) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.insertEvent(conn, newEvent);

            oout.writeObject(success);
            oout.flush();

            incrementDBVersion(success);
        }
    }

    private void editEvent(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        Event newEvent = (Event) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        Event existingEvent = EventManagerDB.getEvent(conn, newEvent.getId());

        if (existingEvent == null) {
            System.err.println("[EventManagerDB] Event with ID " + newEvent.getId() + " not found.");
            serverController.addToConsole("[EventManagerDB] Event with ID " + newEvent.getId() + " not found.");
            oout.writeObject(false);
            oout.flush();
            return;
        }

        synchronized (lock) {
            boolean success = EventManagerDB.editEvent(conn, newEvent);

            oout.writeObject(success);
            oout.flush();

            incrementDBVersion(success);
        }
    }

    private void deleteEvent(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        Event newEvent = (Event) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] deleteEvent ");
        serverController.addToConsole("[ServerThread] deleteEvent");

        //TODO: o erro dá aqui [SQLITE_BUSY] The database file is locked (database is locked)
        if (!EventManagerDB.eventHasAttendences(conn, newEvent.getId())) {
            synchronized (lock) {
                boolean success = EventManagerDB.deleteEvent(conn, newEvent);

                oout.writeObject(success);
                oout.flush();

                incrementDBVersion(success);
            }
        } else {
            System.err.println("[EventManagerDB] Event cannot be deleted because it has attendees.");
            serverController.addToConsole("[EventManagerDB] Event cannot be deleted because it has attendees.");

            oout.writeObject(false);
            oout.flush();
        }
    }

    private void eventHasAttendences(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        int eventId = (int) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        boolean eventHasAttendences = EventManagerDB.eventHasAttendences(conn, eventId);

        oout.writeObject(eventHasAttendences);
        oout.flush();
    }

    private void listAttendances(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        int eventId = (int) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        ArrayList<Attendance> listEvents = EventManagerDB.listAttendancesForEvent(conn, eventId);

        oout.writeObject(listEvents);
        oout.flush();
    }

    private void insertAttendance(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        Attendance newAttendance = (Attendance) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.insertAttendanceEvent(conn, newAttendance.getEventId(), newAttendance.getUsername());

            oout.writeObject(success);
            oout.flush();

            incrementDBVersion(success);
            sendEmail(success, newAttendance.getUsername(), "Attendence Inserted", "Attendence for " + newAttendance.getEventId() + " Inserted");
        }


    }

    private void deleteAttendance(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        Attendance newAttendance = (Attendance) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        synchronized (lock) {
            boolean success = EventManagerDB.deletePresenceFromEvent(conn, newAttendance.getEventId(), newAttendance.getUsername());

            oout.writeObject(success);
            oout.flush();

            incrementDBVersion(success);
            sendEmail(success, newAttendance.getUsername(), "Attendence Deleted", "Attendence from " + newAttendance.getEventId() + " Deleted");
        }
    }

    //EVENT KEY
    private void getEventKey(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        int eventId = (int) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        EventKey eventKey = EventManagerDB.getEventKey(conn, eventId);

        oout.writeObject(eventKey);
        oout.flush();
    }

    private void insertEventKey(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        EventKey eventKey = (EventKey) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        EventKey existingEventKey = EventManagerDB.getEventKey(conn, eventKey.getEventId());

        synchronized (lock) {
            boolean success;

            if (existingEventKey != null) {
                success = EventManagerDB.deleteEventKey(conn, existingEventKey) &&
                        EventManagerDB.insertEventKey(conn, eventKey);
            } else {
                success = EventManagerDB.insertEventKey(conn, eventKey);
            }

            oout.writeObject(success);
            oout.flush();

            incrementDBVersion(success);
        }
    }

    //USER KEY
    private void insertUserKey(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws SQLException, IOException, ClassNotFoundException {
        UserKey userKey = (UserKey) oin.readObject();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

        int eventId = EventManagerDB.getEventId(conn, userKey.getUserKey());

        if (eventId < 0) {
            System.err.println("[ServerThread] Insert User Key Error: Invalid Key");
            serverController.addToConsole("[ServerThread] Insert User Key Error: Invalid Key");
            oout.writeObject(false);
            oout.flush();
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
            oout.writeObject(false);
            oout.flush();
            return;
        }

        if (currentDate.after(startDate.getTime()) && currentDate.before(endDate.getTime())) {
            EventKey eventKey = CodigoRegistoModel.getEventKey(conn, eventId);
            if (eventKey == null) {
                System.err.println("[ServerThread] Insert User Key Error");
                serverController.addToConsole("[ServerThread] Insert User Key Error");
                oout.writeObject(false);
                oout.flush();
                return;
            }
            Date eventKeyEndDate = eventKey.getEndDate();

            if (currentDate.before(eventKeyEndDate)) {
                synchronized (lock) {
                    boolean success = EventManagerDB.insertPresenceForEvent(conn, eventId, userKey.getUsername());

                    oout.writeObject(success);
                    oout.flush();

                    incrementDBVersion(success);
                    sendEmail(success, userKey.getUsername(), "Attendence Inserted", "Attendence for " + eventId + " Inserted");
                }
            } else {
                System.err.println("[ServerThread] Insert User Key Error: Current date is after the event key end date");
                serverController.addToConsole("[ServerThread] Insert User Key Error: Current date is after the event key end date");
                oout.writeObject(false);
                oout.flush();
            }
        } else {
            System.err.println("[ServerThread] Insert User Key Error: Current date is not within the event date range");
            serverController.addToConsole("[ServerThread] Insert User Key Error: Current date is not within the event date range");
            oout.writeObject(false);
            oout.flush();
        }
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
    private void sendEmail(boolean send, String recipientEmail, String subject, String body) {
        if (!send) return;

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
    private void incrementDBVersion(boolean increment) throws SQLException {
        if (!increment) return;

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl);
        System.out.println("[ServerThread] Connection Established to " + dbUrl);
        serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

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
}
