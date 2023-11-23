package pt.isec.eventmanager.db;

import pt.isec.eventmanager.events.EventKey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CodigoRegistoModel {
    public static EventKey getEventKey(Connection conn, int eventId) throws SQLException {
        String query = "SELECT * FROM codigo_registo WHERE evento_id=?";

        PreparedStatement preparedStatement = conn.prepareStatement(query);
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

        return null;
    }

    public static int getEventId(Connection conn, int eventKey) throws SQLException {
        String query = "SELECT evento_id FROM codigo_registo WHERE code=?";

        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setInt(1, eventKey);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            return resultSet.getInt("evento_id");
        }

        return -1;
    }

    public static boolean insertEventKey(Connection conn, EventKey eventKey) throws SQLException {
        String insertEventKeyQuery = "INSERT INTO codigo_registo (code, end_date, evento_id) VALUES (?, ?, ?)";

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

        return false;
    }

    public static boolean deleteEventKey(Connection conn, EventKey eventKey) throws SQLException {
        String deleteEventKeyQuery = "DELETE FROM codigo_registo WHERE evento_id=?";

        PreparedStatement deleteEventKeyStatement = conn.prepareStatement(deleteEventKeyQuery);
        deleteEventKeyStatement.setInt(1, eventKey.getEventId());

        int rowsAffected = deleteEventKeyStatement.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("[EventManagerDB] Event key deleted successfully.");
            return true;
        }

        return false;
    }
}