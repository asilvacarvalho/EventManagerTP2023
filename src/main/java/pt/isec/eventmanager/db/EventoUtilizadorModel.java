package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.server.ServerController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class EventoUtilizadorModel {
    public static ArrayList<Attendance> getPresencesForEvent(Connection conn, int eventId, ServerController controller) {
        ArrayList<Attendance> attendanceEvent = new ArrayList<>();
        String query = "SELECT * FROM evento_utilizador WHERE evento_id=?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, eventId);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String username = resultSet.getString("utilizador_email");

                Attendance attendance = new Attendance(eventId, username);
                attendanceEvent.add(attendance);
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error listing attendances: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] Error listing attendances: " + e.getMessage());
        }

        return attendanceEvent;
    }

    public static ArrayList<Integer> getEventIdsForUser(Connection conn, String username, ServerController controller) {
        ArrayList<Integer> eventIds = new ArrayList<>();
        String eventIdsQuery = "SELECT evento_id FROM evento_utilizador WHERE utilizador_email=?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(eventIdsQuery)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int eventId = resultSet.getInt("evento_id");
                eventIds.add(eventId);
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error getting event IDs for user: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] Error getting event IDs for user: " + e.getMessage());
        }

        return eventIds;
    }


    public static boolean insertUserPresenceForEvent(Connection conn, int eventId, String username, ServerController controller) {
        //TODO: Se for feito fora do período de validade do código ou de realização do evento, a operação
        // falha. O mesmo ocorre se um utilizador introduzir um código relativo a um
        // determinado evento e já possuir presença registada noutro evento a decorrer naquele momemto
        // Fazer um if aqui....

        return insertPresenceForEvent(conn, eventId, username, controller);
    }

    public static boolean insertPresenceForEvent(Connection conn, int eventId, String username, ServerController controller) {
        String query = "INSERT INTO evento_utilizador (evento_id, utilizador_email) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setString(2, username);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[EventManagerDB] Presence inserted successfully.");
                controller.addToConsole("[EventManagerDB] Presence inserted successfully");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error insertingPresenceForEvent: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] Error insertingPresenceForEvent: " + e.getMessage());
        }

        return false;
    }

    public static boolean deletePresenceFromEvent(Connection conn, int eventId, String username, ServerController controller) {
        String query = "DELETE FROM evento_utilizador WHERE evento_id = ? AND utilizador_email = ?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setString(2, username);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[EventManagerDB] Presence deleted successfully.");
                controller.addToConsole("[EventManagerDB] Presence deleted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error deleting presence from event: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] Error deleting presence from event: " + e.getMessage());
        }

        return false;
    }
}
