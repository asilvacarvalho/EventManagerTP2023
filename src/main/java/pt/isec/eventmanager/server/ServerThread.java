package pt.isec.eventmanager.server;

import pt.isec.eventmanager.db.CodigoRegistoModel;
import pt.isec.eventmanager.db.EventManagerDB;
import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
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
    //private int threadNumber;
    private final String dbUrl;

    private Server server;
    private final Object lock = new Object();

    public ServerThread(Socket toClientSocket, int threadNumber, String dbUrl, ServerController controller, Server server) {
        this.serverController = controller;
        this.toClientSocket = toClientSocket;
        //this.threadNumber = threadNumber;
        this.dbUrl = dbUrl;
        this.server = server;
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
        synchronized (lock) {
            User newUser = (User) oin.readObject();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[ServerThread] Connection Established to " + dbUrl);
                serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

                boolean success = EventManagerDB.insertUser(conn, newUser, serverController);

                oout.writeObject(success);
                oout.flush();

                incrementDBVersion(success);

            } catch (SQLException e) {
                System.err.println("[ServerThread] Connection Error: " + e.getMessage());
                serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
            }
        }
    }

    private void editUser(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        synchronized (lock) {
            User newUser = (User) oin.readObject();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[ServerThread] Connection Established to " + dbUrl);
                serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

                boolean success = EventManagerDB.editUser(conn, newUser, serverController);

                oout.writeObject(success);
                oout.flush();

                incrementDBVersion(success);
            } catch (SQLException e) {
                System.err.println("[ServerThread] Connection Error: " + e.getMessage());
                serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
            }
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
        synchronized (lock) {
            Event newEvent = (Event) oin.readObject();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[ServerThread] Connection Established to " + dbUrl);
                serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

                boolean success = EventManagerDB.insertEvent(conn, newEvent, serverController);

                oout.writeObject(success);
                oout.flush();

                incrementDBVersion(success);
            } catch (SQLException e) {
                System.err.println("[ServerThread] Connection Error: " + e.getMessage());
                serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
            }
        }
    }

    private void editEvent(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        synchronized (lock) {
            Event newEvent = (Event) oin.readObject();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[ServerThread] Connection Established to " + dbUrl);
                serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

                boolean success = EventManagerDB.editEvent(conn, newEvent, serverController);

                oout.writeObject(success);
                oout.flush();

                incrementDBVersion(success);
            } catch (SQLException e) {
                System.err.println("[ServerThread] Connection Error: " + e.getMessage());
                serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
            }
        }
    }

    private void deleteEvent(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        synchronized (lock) {
            Event newEvent = (Event) oin.readObject();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[ServerThread] Connection Established to " + dbUrl);
                serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

                boolean success = EventManagerDB.deleteEvent(conn, newEvent, serverController);

                oout.writeObject(success);
                oout.flush();

                incrementDBVersion(success);
            } catch (SQLException e) {
                System.err.println("[ServerThread] Connection Error: " + e.getMessage());
                serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
            }
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
        synchronized (lock) {
            Attendance newAttendance = (Attendance) oin.readObject();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[ServerThread] Connection Established to " + dbUrl);
                serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

                boolean success = EventManagerDB.insertAttendanceEvent(conn, newAttendance.getEventId(), newAttendance.getUsername(), serverController);

                oout.writeObject(success);
                oout.flush();

                incrementDBVersion(success);
                sendEmail(success, newAttendance.getUsername(), "Attendence Inserted", "Attendence for " + newAttendance.getEventId() + " Inserted");
            } catch (SQLException e) {
                System.err.println("[ServerThread] Connection Error: " + e.getMessage());
                serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
            }
        }
    }

    private void deleteAttendance(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        synchronized (lock) {
            Attendance newAttendance = (Attendance) oin.readObject();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[ServerThread] Connection Established to " + dbUrl);
                serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

                boolean success = EventManagerDB.deletePresenceFromEvent(conn, newAttendance.getEventId(), newAttendance.getUsername(), serverController);

                oout.writeObject(success);
                oout.flush();

                incrementDBVersion(success);
                sendEmail(success, newAttendance.getUsername(), "Attendence Deleted", "Attendence from " + newAttendance.getEventId() + " Deleted");
            } catch (SQLException e) {
                System.err.println("[ServerThread] Connection Error: " + e.getMessage());
                serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
            }
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
        synchronized (lock) {
            EventKey eventKey = (EventKey) oin.readObject();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[ServerThread] Connection Established to " + dbUrl);
                serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

                boolean success = EventManagerDB.insertEventKey(conn, eventKey, serverController);

                oout.writeObject(success);
                oout.flush();

                incrementDBVersion(success);
            } catch (SQLException e) {
                System.err.println("[ServerThread] Connection Error: " + e.getMessage());
                serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
            }
        }
    }

    //USER KEY
    private void insertUserKey(ObjectInputStream oin, ObjectOutputStream oout, String dbUrl) throws Exception {
        //TODO: partir isto, está a ficar gigante...
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
                // Verificar se a data atual está entre start_date e end_date do evento
                Date currentDate = new Date();
                Event event = EventManagerDB.getEvent(conn, eventId, serverController);
                Date eventDate = event.getDate();
                String startTime = event.getStartTime();
                String endTime = event.getEndTime();

                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

                Calendar startDate;
                Calendar endDate;

                try {
                    Date startDateTime = timeFormat.parse(startTime);
                    Date endDateTime = timeFormat.parse(endTime);

                    startDate = Calendar.getInstance();
                    startDate.setTime(eventDate);
                    startDate.set(Calendar.HOUR_OF_DAY, startDateTime.getHours());
                    startDate.set(Calendar.MINUTE, startDateTime.getMinutes());
                    startDate.set(Calendar.SECOND, 0);

                    endDate = Calendar.getInstance();
                    endDate.setTime(eventDate);
                    endDate.set(Calendar.HOUR_OF_DAY, endDateTime.getHours());
                    endDate.set(Calendar.MINUTE, endDateTime.getMinutes());
                    endDate.set(Calendar.SECOND, 0);
                } catch (ParseException e) {
                    System.err.println("[ServerThread] Insert User Key Error: " + e.getMessage());
                    serverController.addToConsole("[ServerThread] Insert User Key Error: " + e.getMessage());
                    oout.writeObject(false);
                    oout.flush();
                    return;
                }

                if (currentDate.after(startDate.getTime()) && currentDate.before(endDate.getTime())) {
                    // Verificar se a data atual é antes da end_date do codigo_registo com o code = userKey.getUserKey()
                    EventKey eventKey = CodigoRegistoModel.getEventKey(conn, eventId, serverController);
                    assert eventKey != null;
                    Date eventKeyEndDate = eventKey.getEndDate();

                    if (currentDate.before(eventKeyEndDate)) {
                        synchronized (lock) {
                            boolean success = EventManagerDB.insertPresenceForEvent(conn, eventId, userKey.getUsername(), serverController);

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
        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

    //EMAIL - TODO: o google não deixa a conta ter acesso a ligações menos seguras...
    public void sendEmail(boolean send, String recipientEmail, String subject, String body) {
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

            System.out.println("E-mail enviado com sucesso para: " + recipientEmail);
            serverController.addToConsole("E-mail enviado com sucesso para: " + recipientEmail);
        } catch (MessagingException e) {
            System.out.println("Erro ao enviar e-mail: " + e.getMessage());
            serverController.addToConsole("Erro ao enviar e-mail: " + e.getMessage());
        }
    }

    //DB
    private void incrementDBVersion(boolean increment) {
        if (!increment) return;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
            System.out.println("[ServerThread] Connection Established to " + dbUrl);
            serverController.addToConsole("[ServerThread] Connection Established to " + dbUrl);

            int newDBVersion = EventManagerDB.incrementDBVersion(conn, serverController);

            if (newDBVersion > 0) {
                System.out.println("[ServerThread] DBVersion Increment Success");
                serverController.addToConsole("[ServerThread] DBVersion Increment Success");
                server.incrementDbVersion(newDBVersion);
            } else {
                System.out.println("[ServerThread] DBVersion Increment Error");
                serverController.addToConsole("[ServerThread] DBVersion Increment Error");
            }

        } catch (SQLException e) {
            System.err.println("[ServerThread] Connection Error: " + e.getMessage());
            serverController.addToConsole("[ServerThread] Connection Error: " + e.getMessage());
        }
    }

}
