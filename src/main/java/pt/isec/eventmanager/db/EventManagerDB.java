package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.server.ServerController;
import pt.isec.eventmanager.users.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class EventManagerDB {
    //TABLE CREATION
    public static void createTables(Connection conn) {
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
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] SQL Exception in createTables: " + e.getMessage());
        }
    }

    public static void createAdmin(Connection conn) {
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
                } else {
                    System.err.println("[EventManagerDB] Error creating admin user.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] SQL Exception in createAdmin: " + e.getMessage());
        }
    }

    //UTILIZADOR
    public static User authenticateUser(Connection conn, User user) {
        return UtilizadorModel.authenticateUser(conn, user);
    }

    public static boolean insertUser(Connection conn, User user) {
        return UtilizadorModel.insertUser(conn, user);
    }

    public static User getUser(Connection conn, String username) {
        return UtilizadorModel.getUser(conn, username);
    }

    //EVENTO
    public static Event getEvent(Connection conn, int eventId) {
        return EventoModel.getEvent(conn, eventId);
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

    public static boolean eventHasAttendences(Connection conn, int eventId) {
        return EventoModel.eventHasAttendences(conn, eventId);
    }

    //CODIGO_REGISTO
    public static EventKey getEventKey(Connection conn, int eventId) {
        return CodigoRegistoModel.getEventKey(conn, eventId);
    }

    public static boolean insertEventKey(Connection conn, EventKey eventKey) {
        return CodigoRegistoModel.insertEventKey(conn, eventKey);
    }

    public static boolean deleteEventKey(Connection coon, EventKey eventKey) {
        return CodigoRegistoModel.deleteEventKey(coon, eventKey);
    }

    //EVENTO_UTILIZADOR
    public static ArrayList<User> getPresencesForEvent(Connection conn, Event event) {
        return EventoUtilizadorModel.getPresencesForEvent(conn, event);
    }

    public static boolean insertPresencesForEvent(Connection conn, int eventId, String username) {
        return EventoUtilizadorModel.insertPresenceForEvent(conn, eventId, username);
    }

    public static boolean deletePresenceFromEvent(Connection conn, int eventId, String username) {
        return EventoUtilizadorModel.deletePresenceFromEvent(conn, eventId, username);
    }
}