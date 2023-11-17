package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.server.ServerController;
import pt.isec.eventmanager.users.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class EventManagerDB {
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
                            "   duration    INTEGER," +
                            "   active      BOOL," +
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

    public static User authenticateUser(Connection conn, User user) {
        String query = "SELECT * FROM utilizador WHERE email=? AND password=?";
        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setString(1, user.getEmail());
            preparedStatement.setString(2, user.getPassword());

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new User(
                        resultSet.getString("email"),
                        resultSet.getString("password"),
                        resultSet.getString("name"),
                        resultSet.getString("student_number"),
                        resultSet.getBoolean("admin")
                );
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error in authenticateUser: " + e.getMessage());
        }
        return null;
    }

    public static boolean insertUser(Connection conn, User user) {
        String queryCheckEmail = "SELECT COUNT(*) FROM utilizador WHERE email = ?";
        String queryInsertUser = "INSERT INTO utilizador (email, password, name, student_number, admin) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement checkEmailStatement = conn.prepareStatement(queryCheckEmail);
             PreparedStatement insertUserStatement = conn.prepareStatement(queryInsertUser)) {

            // Verifica se o email já existe na tabela
            checkEmailStatement.setString(1, user.getEmail());
            ResultSet resultSet = checkEmailStatement.executeQuery();
            if (resultSet.getInt(1) > 0) {
                System.err.println("[EventManagerDB] Error inserting user, email already exists");
                return false;
            }

            insertUserStatement.setString(1, user.getEmail());
            insertUserStatement.setString(2, user.getPassword());
            insertUserStatement.setString(3, user.getName());
            insertUserStatement.setString(4, user.getStudentNumber());
            insertUserStatement.setBoolean(5, false);

            int rowsAffected = insertUserStatement.executeUpdate();

            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error inserting user: " + e.getMessage());
        }

        return false;
    }

    public static boolean insertEvent(Connection conn, Event event, ServerController controller) {
        String insertEventQuery = "INSERT INTO evento (name, location, start_date, end_date) VALUES (?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = conn.prepareStatement(insertEventQuery)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String formattedDate = dateFormat.format(event.getDate());

            String startDateString = formattedDate + " " + event.getStartTime() + ":00";
            String endDateString = formattedDate + " " + event.getEndTime() + ":00";

            preparedStatement.setString(1, event.getName());
            preparedStatement.setString(2, event.getLocation());
            preparedStatement.setString(3, startDateString);
            preparedStatement.setString(4, endDateString);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[EventManagerDB] Event inserted successfully.");
                controller.addToConsole("[EventManagerDB] Event inserted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error inserting event: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] Error inserting event: " + e.getMessage());
        }
        return false;
    }

    public static ArrayList<Event> listEvents(Connection conn, ServerController controller) {
        ArrayList<Event> events = new ArrayList<>();
        String query = "SELECT * FROM evento";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String location = resultSet.getString("location");
                Date date = new Date(resultSet.getTimestamp("start_date").getTime());

                Date startDate = resultSet.getTimestamp("start_date");
                Date endDate = resultSet.getTimestamp("end_date");


                String startTimeString = new SimpleDateFormat("HH:mm").format(startDate);
                String endTimeString = new SimpleDateFormat("HH:mm").format(endDate);

                Event event = new Event(name, location, date, startTimeString, endTimeString);
                event.setId(id);
                events.add(event);
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error listing events: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] Error listing events: " + e.getMessage());
        }

        return events;
    }

    public static boolean editEvent(Connection conn, Event event, ServerController controller) {
        String checkEventQuery = "SELECT * FROM evento WHERE id=?";
        String updateEventQuery;

        try (PreparedStatement checkEventStatement = conn.prepareStatement(checkEventQuery)) {
            // Verifica se o evento com o ID fornecido existe na tabela evento
            checkEventStatement.setInt(1, event.getId());
            ResultSet eventResultSet = checkEventStatement.executeQuery();

            if (!eventResultSet.next()) {
                System.err.println("[EventManagerDB] Event with ID " + event.getId() + " not found.");
                controller.addToConsole("[EventManagerDB] Event with ID " + event.getId() + " not found.");
                return false;
            }

            updateEventQuery = "UPDATE evento SET name=?, location=?, start_date=?, end_date=? WHERE id=?";

            try (PreparedStatement updateEventStatement = conn.prepareStatement(updateEventQuery)) {
                updateEventStatement.setString(1, event.getName());
                updateEventStatement.setString(2, event.getLocation());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String formattedDate = dateFormat.format(event.getDate());

                String startDateString = formattedDate + " " + event.getStartTime() + ":00";
                String endDateString = formattedDate + " " + event.getEndTime() + ":00";

                updateEventStatement.setString(3, startDateString);
                updateEventStatement.setString(4, endDateString);
                updateEventStatement.setInt(5, event.getId());

                int rowsAffected = updateEventStatement.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("[EventManagerDB] Event updated successfully.");
                    controller.addToConsole("[EventManagerDB] Event updated successfully.");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error editing event: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] Error editing event: " + e.getMessage());
        }
        return false;
    }

    public static boolean eventHasAttendences(Connection conn, int eventId) {
        String checkEventUserQuery = "SELECT * FROM evento_utilizador WHERE evento_id=?";

        try (PreparedStatement checkEventUserStatement = conn.prepareStatement(checkEventUserQuery)) {
            checkEventUserStatement.setInt(1, eventId);
            ResultSet eventUserResultSet = checkEventUserStatement.executeQuery();

            return eventUserResultSet.next();
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error checking event-user association: " + e.getMessage());
        }
        return false;
    }
}

