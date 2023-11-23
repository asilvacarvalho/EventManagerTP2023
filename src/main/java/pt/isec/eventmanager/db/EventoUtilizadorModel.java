package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.Attendance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class EventoUtilizadorModel {
    public static ArrayList<Attendance> getPresencesForEvent(Connection conn, int eventId) throws SQLException {
        ArrayList<Attendance> attendanceEvent = new ArrayList<>();
        String query = "SELECT * FROM evento_utilizador WHERE evento_id=?";

        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setInt(1, eventId);
        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            String username = resultSet.getString("utilizador_email");

            Attendance attendance = new Attendance(eventId, username);
            attendanceEvent.add(attendance);
        }

        return attendanceEvent;
    }

    public static ArrayList<Integer> getEventIdsForUser(Connection conn, String username) throws SQLException {
        ArrayList<Integer> eventIds = new ArrayList<>();
        String eventIdsQuery = "SELECT evento_id FROM evento_utilizador WHERE utilizador_email=?";

        PreparedStatement preparedStatement = conn.prepareStatement(eventIdsQuery);
        preparedStatement.setString(1, username);
        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            int eventId = resultSet.getInt("evento_id");
            eventIds.add(eventId);
        }


        return eventIds;
    }

    public static boolean insertUserPresenceForEvent(Connection conn, int eventId, String username) throws SQLException {
        //Dois métodos iguais apenas por uma questão de eligibilidate, este vem sempre do user o outro vem do admin
        return insertPresenceForEvent(conn, eventId, username);
    }

    public static boolean insertPresenceForEvent(Connection conn, int eventId, String username) throws SQLException {
        String query = "INSERT INTO evento_utilizador (evento_id, utilizador_email) VALUES (?, ?)";

        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setInt(1, eventId);
        preparedStatement.setString(2, username);

        int rowsAffected = preparedStatement.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("[EventManagerDB] Presence inserted successfully.");
            return true;
        }

        return false;
    }

    public static boolean deletePresenceFromEvent(Connection conn, int eventId, String username) throws SQLException {
        String query = "DELETE FROM evento_utilizador WHERE evento_id = ? AND utilizador_email = ?";

        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setInt(1, eventId);
        preparedStatement.setString(2, username);

        int rowsAffected = preparedStatement.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("[EventManagerDB] Presence deleted successfully.");
            return true;
        }

        return false;
    }
}
