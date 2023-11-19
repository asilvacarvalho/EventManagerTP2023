package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.server.ServerController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class EventoModel {
    public static Event getEvent(Connection conn, int eventId) {
        String query = "SELECT * FROM evento WHERE id=?";
        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, eventId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
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

                return event;
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error getting User: " + e.getMessage());
        }
        return null;
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

    public static boolean deleteEvent(Connection conn, Event event, ServerController controller) {
        String deleteEventQuery = "DELETE FROM evento WHERE id=?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(deleteEventQuery)) {
            preparedStatement.setInt(1, event.getId());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[EventManagerDB] Event deleted successfully.");
                controller.addToConsole("[EventManagerDB] Event deleted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error deleting event: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] Error deleting event: " + e.getMessage());
        }
        return false;
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
