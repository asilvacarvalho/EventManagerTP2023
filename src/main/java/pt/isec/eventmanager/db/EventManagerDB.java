package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.users.User;

import java.io.Serial;
import java.io.Serializable;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

//TODO: avisar os servidores de backup das atualizações

public class EventManagerDB implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    //TABLE CREATION
    public static void createTables(Connection conn) throws SQLException {
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

        // Criar tabela db_version para controlar a versão do banco de dados
        conn.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS db_version (" +
                        "   id          INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "   dbversion   INT" +
                        ")"
        );

        // Inserir a versão inicial (0) na tabela db_version
        conn.createStatement().executeUpdate(
                "INSERT INTO db_version (dbversion) VALUES (0)"
        );

        System.out.println("[EventManagerDB] Tables created successfully.");
    }

    public static boolean createAdmin(Connection conn) throws SQLException {
        String insertAdminQuery = "INSERT INTO utilizador (email, password, name, student_number, admin) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement preparedStatement = conn.prepareStatement(insertAdminQuery);
        preparedStatement.setString(1, "admin");
        preparedStatement.setString(2, "admin");
        preparedStatement.setString(3, "admin");
        preparedStatement.setString(4, "admin");
        preparedStatement.setBoolean(5, true);  // admin=true

        // Execute the update
        int rowsAffected = preparedStatement.executeUpdate();

        return rowsAffected > 0;
    }

    public static int getDBVersion(Connection conn) throws SQLException {
        int dbVersion = -1;

        String query = "SELECT dbversion FROM db_version";
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            dbVersion = resultSet.getInt("dbversion");
            System.out.println("[EventManagerDB] Current database version: " + dbVersion);
        } else {
            System.err.println("[EventManagerDB] No rows found in db_version table.");
        }

        resultSet.close();
        preparedStatement.close();

        return dbVersion;
    }

    public static void createDummyData(Connection conn) {
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
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] SQL Exception in createDummyData: " + e.getMessage());
        }
    }

    public static int incrementDBVersion(Connection conn) throws SQLException {
        int currentVersion = getDBVersion(conn);
        int newVersion = currentVersion + 1;

        String updateQuery = "UPDATE db_version SET dbversion = ?";
        PreparedStatement preparedStatement = conn.prepareStatement(updateQuery);
        preparedStatement.setInt(1, newVersion);
        int rowsAffected = preparedStatement.executeUpdate();
        preparedStatement.close();

        if (rowsAffected > 0) {
            System.out.println("[EventManagerDB] Database version incremented to: " + newVersion);
            return newVersion;
        } else {
            System.err.println("[EventManagerDB] Failed to increment database version.");
            return -1;
        }
    }

    //UTILIZADOR
    public static User authenticateUser(Connection conn, User user) throws SQLException {
        return UtilizadorModel.authenticateUser(conn, user);
    }

    public static User getUser(Connection conn, String username) throws SQLException {
        return UtilizadorModel.getUser(conn, username);
    }

    public static boolean insertUser(Connection conn, User user) throws SQLException {
        return UtilizadorModel.insertUser(conn, user);
    }

    public static boolean editUser(Connection conn, User user) throws SQLException {
        return UtilizadorModel.editUser(conn, user);
    }

    public static ArrayList<Event> listUserEvents(Connection conn, ArrayList<Integer> eventsIds) throws SQLException {
        return EventoModel.listUserEvents(conn, eventsIds);
    }

    //EVENTO
    public static Event getEvent(Connection conn, int eventId) throws SQLException {
        return EventoModel.getEvent(conn, eventId);
    }

    public static boolean insertEvent(Connection conn, Event event) throws SQLException {
        return EventoModel.insertEvent(conn, event);
    }

    public static boolean deleteEvent(Connection conn, Event event) throws SQLException {
        return EventoModel.deleteEvent(conn, event);
    }

    public static boolean editEvent(Connection conn, Event event) throws SQLException {
        return EventoModel.editEvent(conn, event);
    }

    public static ArrayList<Event> listEvents(Connection conn) throws SQLException {
        return EventoModel.listEvents(conn);
    }

    public static boolean eventHasAttendences(Connection conn, int eventId) throws SQLException {
        return EventoModel.eventHasAttendences(conn, eventId);
    }

    public static ArrayList<Attendance> listAttendancesForEvent(Connection conn, int eventId) throws SQLException {
        return EventoUtilizadorModel.getPresencesForEvent(conn, eventId);
    }

    public static boolean insertAttendanceEvent(Connection conn, int eventId, String username) throws SQLException {
        return EventoUtilizadorModel.insertPresenceForEvent(conn, eventId, username);
    }

    //CODIGO_REGISTO
    public static EventKey getEventKey(Connection conn, int eventId) throws SQLException {
        return CodigoRegistoModel.getEventKey(conn, eventId);
    }

    public static int getEventId(Connection conn, int eventKey) throws SQLException {
        return CodigoRegistoModel.getEventId(conn, eventKey);
    }

    public static boolean insertEventKey(Connection conn, EventKey eventKey) throws SQLException {
        return CodigoRegistoModel.insertEventKey(conn, eventKey);
    }

    public static boolean deleteEventKey(Connection coon, EventKey eventKey) throws SQLException {
        return CodigoRegistoModel.deleteEventKey(coon, eventKey);
    }

    //EVENTO_UTILIZADOR
    public static boolean insertPresenceForEvent(Connection conn, int eventId, String username) throws SQLException {
        return EventoUtilizadorModel.insertUserPresenceForEvent(conn, eventId, username);
    }

    public static boolean deletePresenceFromEvent(Connection conn, int eventId, String username) throws SQLException {
        return EventoUtilizadorModel.deletePresenceFromEvent(conn, eventId, username);
    }
}