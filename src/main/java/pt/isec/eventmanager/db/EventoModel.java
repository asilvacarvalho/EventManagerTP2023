package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class EventoModel {
    public static Event getEvent(Connection conn, int eventId) throws SQLException {
        String query = "SELECT * FROM evento WHERE id=?";
        PreparedStatement preparedStatement = conn.prepareStatement(query);
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

        return null;
    }

    public static boolean insertEvent(Connection conn, Event event) throws SQLException {
        String insertEventQuery = "INSERT INTO evento (name, location, start_date, end_date) VALUES (?, ?, ?, ?)";

        PreparedStatement preparedStatement = conn.prepareStatement(insertEventQuery);
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
            return true;
        }

        return false;
    }

    public static boolean deleteEvent(Connection conn, Event event) throws SQLException {
        int eventId = event.getId();

        String deleteEventQuery = "DELETE FROM evento WHERE id=?";
        PreparedStatement preparedStatement = conn.prepareStatement(deleteEventQuery);
        preparedStatement.setInt(1, eventId);

        int rowsAffected = preparedStatement.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("[EventManagerDB] Event deleted successfully.");
            return true;
        }

        return false;
    }

    public static boolean editEvent(Connection conn, Event event) throws SQLException {
        String updateEventQuery = "UPDATE evento SET name=?, location=?, start_date=?, end_date=? WHERE id=?";

        PreparedStatement updateEventStatement = conn.prepareStatement(updateEventQuery);
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
            return true;
        }

        return false;
    }

    public static ArrayList<Event> listEvents(Connection conn) throws SQLException {
        ArrayList<Event> events = new ArrayList<>();
        String query = "SELECT * FROM evento";

        PreparedStatement preparedStatement = conn.prepareStatement(query);
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


        return events;
    }

    public static ArrayList<Event> listUserEvents(Connection conn, ArrayList<Integer> eventIds) throws SQLException {
        ArrayList<Event> events = new ArrayList<>();

        StringBuilder eventsQuery = new StringBuilder("SELECT * FROM evento WHERE id IN (");
        for (int i = 0; i < eventIds.size(); i++) {
            eventsQuery.append("?");
            if (i != eventIds.size() - 1) {
                eventsQuery.append(",");
            }
        }
        eventsQuery.append(")");

        PreparedStatement eventsStatement = conn.prepareStatement(eventsQuery.toString());
        for (int i = 0; i < eventIds.size(); i++) {
            eventsStatement.setInt(i + 1, eventIds.get(i));
        }

        ResultSet eventsResultSet = eventsStatement.executeQuery();

        while (eventsResultSet.next()) {
            int id = eventsResultSet.getInt("id");
            String name = eventsResultSet.getString("name");
            String location = eventsResultSet.getString("location");
            Date date = new Date(eventsResultSet.getTimestamp("start_date").getTime());

            Date startDate = eventsResultSet.getTimestamp("start_date");
            Date endDate = eventsResultSet.getTimestamp("end_date");

            String startTimeString = new SimpleDateFormat("HH:mm").format(startDate);
            String endTimeString = new SimpleDateFormat("HH:mm").format(endDate);

            Event event = new Event(name, location, date, startTimeString, endTimeString);
            event.setId(id);
            events.add(event);
        }

        return events;
    }

    public static boolean eventHasAttendences(Connection conn, int eventId) throws SQLException {
        String checkEventUserQuery = "SELECT * FROM evento_utilizador WHERE evento_id=?";

        PreparedStatement checkEventUserStatement = conn.prepareStatement(checkEventUserQuery);
        checkEventUserStatement.setInt(1, eventId);
        ResultSet eventUserResultSet = checkEventUserStatement.executeQuery();

        return eventUserResultSet.next();
    }
}
