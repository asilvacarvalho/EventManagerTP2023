package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.EventKey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CodigoRegistoModel {
    public static EventKey getEventKey(Connection conn, int eventId) {
        String query = "SELECT * FROM codigo_registo WHERE evento_id=?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, eventId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                EventKey eventKey = new EventKey();
                eventKey.setId(resultSet.getInt("id"));
                eventKey.setCode(resultSet.getInt("code"));
                eventKey.setEventId(resultSet.getInt("evento_id"));

                Date endDate = new Date(resultSet.getTimestamp("end_date").getTime());
                eventKey.setEndDate(endDate);

                return eventKey;
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error getting event key: " + e.getMessage());
        }
        return null;
    }

    public static int getEventId(Connection conn, int eventKey) {
        String query = "SELECT evento_id FROM codigo_registo WHERE code=?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, eventKey);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("evento_id");
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error getting event key: " + e.getMessage());
        }
        return -1;
    }

    public static boolean insertEventKey(Connection conn, EventKey eventKey) {
        String deleteIfExistsQuery = "DELETE FROM codigo_registo WHERE evento_id=?";
        String insertEventKeyQuery = "INSERT INTO codigo_registo (code, end_date, evento_id) VALUES (?, ?, ?)";

        try {
            PreparedStatement deleteIfExistsStatement = conn.prepareStatement(deleteIfExistsQuery);
            deleteIfExistsStatement.setInt(1, eventKey.getEventId());
            deleteIfExistsStatement.executeUpdate();

            PreparedStatement insertEventKeyStatement = conn.prepareStatement(insertEventKeyQuery);
            insertEventKeyStatement.setInt(1, eventKey.getCode());

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String formattedDate = dateFormat.format(eventKey.getEndDate());
            String endDateString = formattedDate + ":00";

            insertEventKeyStatement.setString(2, endDateString);
            insertEventKeyStatement.setInt(3, eventKey.getEventId());

            int rowsAffected = insertEventKeyStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[EventManagerDB] Event key inserted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error inserting event key: " + e.getMessage());
        }
        return false;
    }

    public static boolean deleteEventKey(Connection coon, EventKey eventKey) {
        return false;
    }
}
