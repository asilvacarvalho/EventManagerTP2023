package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.users.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class EventoUtilizadorModel {
    public static ArrayList<User> getPresencesForEvent(Connection conn, Event event) {
        ArrayList<User> usersOnEvent = new ArrayList<>();
        String query = "SELECT * FROM evento_utilizador WHERE evento_id=?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, event.getId());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String username = resultSet.getString("utilizador_email");

                User user = UtilizadorModel.getUser(conn, username);
                usersOnEvent.add(user);
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error getting event key: " + e.getMessage());
        }

        return usersOnEvent;
    }

    public static boolean insertPresenceForEvent(Connection conn, int eventId, String username) {
        String query = "INSERT INTO evento_utilizador (evento_id, utilizador_email) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setString(2, username);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[EventManagerDB] Presence inserted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error getting event key: " + e.getMessage());
        }

        return false;
    }

    public static boolean deletePresenceFromEvent(Connection conn, int eventId, String username) {
        String query = "DELETE FROM evento_utilizador WHERE evento_id = ? AND utilizador_email = ?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setString(2, username);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[EventManagerDB] Presence deleted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error deleting presence from event: " + e.getMessage());
        }

        return false;
    }
}
