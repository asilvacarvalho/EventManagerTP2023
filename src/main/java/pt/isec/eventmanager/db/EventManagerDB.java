package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.server.ServerController;
import pt.isec.eventmanager.users.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

//TODO: talvez não seja aqui, mas sempre que efetuamos uma operaçao na BD temos que avisar o client

public class EventManagerDB {
    //TABLE CREATION
    public static void createTables(Connection conn, ServerController controller) {
        try {
            // Criar tabela utilizador
            conn.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS utilizador (" +
                            "   email         TEXT," +
                            "   password      TEXT," +
                            "   name          TEXT," +
                            "   student_number TEXT," +
                            "   admin         BOOL DEFAULT false," +
                            "   PRIMARY KEY(email)" +
                            ")"
            );

            // Criar tabela evento
            conn.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS evento (" +
                            "   id          INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "   name        TEXT," +
                            "   location    TEXT," +
                            "   start_date  TIMESTAMP," +
                            "   end_date    TIMESTAMP" +
                            ")"
            );

            // Criar tabela codigo_registo
            conn.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS codigo_registo (" +
                            "   id          INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "   code        BIGINT," +
                            "   end_date    TIMESTAMP," +
                            "   evento_id   BIGINT NOT NULL," +
                            "   FOREIGN KEY (evento_id) REFERENCES evento(id)" +
                            ")"
            );

            // Criar tabela evento_utilizador
            conn.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS evento_utilizador (" +
                            "   evento_id           BIGINT," +
                            "   utilizador_email    TEXT," +
                            "   PRIMARY KEY(evento_id, utilizador_email)," +
                            "   FOREIGN KEY (evento_id) REFERENCES evento(id)," +
                            "   FOREIGN KEY (utilizador_email) REFERENCES utilizador(email)" +
                            ")"
            );

            System.out.println("[EventManagerDB] Tables created successfully.");
            controller.addToConsole("[EventManagerDB] Tables created successfully.");
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] SQL Exception in createTables: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] SQL Exception in createTables: " + e.getMessage());
        }
    }

    public static void createAdmin(Connection conn, ServerController controller) {
        try {
            String insertAdminQuery = "INSERT INTO utilizador (email, password, name, student_number, admin) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement preparedStatement = conn.prepareStatement(insertAdminQuery)) {
                preparedStatement.setString(1, "admin");
                preparedStatement.setString(2, "admin");
                preparedStatement.setString(3, "admin");
                preparedStatement.setString(4, "admin");
                preparedStatement.setBoolean(5, true);  // admin=true

                // Execute the update
                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("[EventManagerDB] Admin user created successfully.");
                    controller.addToConsole("[EventManagerDB] Admin user created successfully.");
                } else {
                    System.err.println("[EventManagerDB] Error creating admin user.");
                    controller.addToConsole("[EventManagerDB] Error creating admin user.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] SQL Exception in createAdmin: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] SQL Exception in createAdmin: " + e.getMessage());
        }
    }

    public static void createDummyData(Connection conn, ServerController controller) {
        try {
            // Inserir 10 utilizadores
            for (int i = 1; i <= 10; i++) {
                String insertUserQuery = "INSERT INTO utilizador (email, password, name, student_number, admin) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement userStatement = conn.prepareStatement(insertUserQuery)) {
                    userStatement.setString(1, String.valueOf(i));
                    userStatement.setString(2, String.valueOf(i));
                    userStatement.setString(3, String.valueOf(i));
                    userStatement.setString(4, String.valueOf(i));
                    userStatement.setBoolean(5, false);

                    userStatement.executeUpdate();
                }
            }

            // Inserir 10 eventos
            LocalDateTime startDate = LocalDateTime.now().withHour(11).withMinute(0).withSecond(0).withNano(0);
            for (int i = 1; i <= 10; i++) {
                String insertEventQuery = "INSERT INTO evento (name, location, start_date, end_date) VALUES (?, ?, ?, ?)";

                try (PreparedStatement eventStatement = conn.prepareStatement(insertEventQuery)) {
                    eventStatement.setString(1, String.valueOf((char) ('a' + i - 1)));
                    eventStatement.setString(2, "Location " + i);
                    eventStatement.setTimestamp(3, Timestamp.valueOf(startDate));
                    eventStatement.setTimestamp(4, Timestamp.valueOf(startDate.plusHours(1)));

                    eventStatement.executeUpdate();
                }

                startDate = startDate.plusDays(1); // Próximo evento no próximo dia
            }

            // Inserir utilizadores nos eventos
            for (int i = 1; i <= 4; i++) {
                String insertEventUserQuery = "INSERT INTO evento_utilizador (evento_id, utilizador_email) VALUES (?, ?)";
                try (PreparedStatement eventUserStatement = conn.prepareStatement(insertEventUserQuery)) {
                    eventUserStatement.setLong(1, 1); // Primeiro evento
                    eventUserStatement.setString(2, String.valueOf(i));
                    eventUserStatement.executeUpdate();
                }
            }

            for (int i = 5; i <= 9; i++) {
                String insertEventUserQuery = "INSERT INTO evento_utilizador (evento_id, utilizador_email) VALUES (?, ?)";
                try (PreparedStatement eventUserStatement = conn.prepareStatement(insertEventUserQuery)) {
                    eventUserStatement.setLong(1, 2); // Segundo evento
                    eventUserStatement.setString(2, String.valueOf(i));
                    eventUserStatement.executeUpdate();
                }
            }

            System.out.println("[EventManagerDB] Dummy data created successfully.");
            controller.addToConsole("[EventManagerDB] Dummy data created successfully.");
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] SQL Exception in createDummyData: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] SQL Exception in createDummyData: " + e.getMessage());
        }
    }

    //UTILIZADOR
    public static User authenticateUser(Connection conn, User user, ServerController controller) {
        return UtilizadorModel.authenticateUser(conn, user, controller);
    }

    public static User getUser(Connection conn, String username, ServerController controller) {
        return UtilizadorModel.getUser(conn, username, controller);
    }

    public static boolean insertUser(Connection conn, User user, ServerController controller) {
        return UtilizadorModel.insertUser(conn, user, controller);
    }

    public static boolean editUser(Connection conn, User user, ServerController controller) {
        return UtilizadorModel.editUser(conn, user, controller);
    }

    public static ArrayList<Event> listUserEvents(Connection conn, String username, ServerController controller) {
        return EventoModel.listUserEvents(conn, username, controller);
    }

    //EVENTO
    public static Event getEvent(Connection conn, int eventId, ServerController controller) {
        return EventoModel.getEvent(conn, eventId, controller);
    }

    public static boolean insertEvent(Connection conn, Event event, ServerController controller) {
        return EventoModel.insertEvent(conn, event, controller);
    }

    public static boolean deleteEvent(Connection conn, Event event, ServerController controller) {
        return EventoModel.deleteEvent(conn, event, controller);
    }

    public static boolean editEvent(Connection conn, Event event, ServerController controller) {
        return EventoModel.editEvent(conn, event, controller);
    }

    public static ArrayList<Event> listEvents(Connection conn, ServerController controller) {
        return EventoModel.listEvents(conn, controller);
    }

    public static boolean eventHasAttendences(Connection conn, int eventId, ServerController controller) {
        return EventoModel.eventHasAttendences(conn, eventId, controller);
    }

    public static ArrayList<Attendance> listAttendancesForEvent(Connection conn, int eventId, ServerController controller) {
        return EventoUtilizadorModel.getPresencesForEvent(conn, eventId, controller);
    }

    public static boolean insertAttendanceEvent(Connection conn, int eventId, String username, ServerController controller) {
        //TODO: avisar o utilizador por email (username)
        return EventoUtilizadorModel.insertPresenceForEvent(conn, eventId, username, controller);
    }

    //CODIGO_REGISTO
    public static EventKey getEventKey(Connection conn, int eventId, ServerController controller) {
        return CodigoRegistoModel.getEventKey(conn, eventId, controller);
    }

    public static boolean insertEventKey(Connection conn, EventKey eventKey, ServerController controller) {
        return CodigoRegistoModel.insertEventKey(conn, eventKey, controller);
    }

    public static boolean deleteEventKey(Connection coon, EventKey eventKey, ServerController controller) {
        return CodigoRegistoModel.deleteEventKey(coon, eventKey, controller);
    }

    //EVENTO_UTILIZADOR
    public static boolean insertPresenceForEvent(Connection conn, int eventId, String username, ServerController controller) {
        //TODO: avisar o utilizador por email (username)
        return EventoUtilizadorModel.insertUserPresenceForEvent(conn, eventId, username, controller);
    }

    public static boolean deletePresenceFromEvent(Connection conn, int eventId, String username, ServerController controller) {
        //TODO: avisar o utilizador por email (username)

        return EventoUtilizadorModel.deletePresenceFromEvent(conn, eventId, username, controller);
    }
}