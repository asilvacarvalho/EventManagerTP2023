package pt.isec.eventmanager.db;

import pt.isec.eventmanager.users.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UtilizadorModel {
    public static User authenticateUser(Connection conn, User user) throws SQLException {
        String query = "SELECT * FROM utilizador WHERE email=? AND password=?";
        PreparedStatement preparedStatement = conn.prepareStatement(query);
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
        return null;
    }

    public static User getUser(Connection conn, String username) throws SQLException {
        String query = "SELECT * FROM utilizador WHERE email=?";
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setString(1, username);

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

        return null;
    }

    public static boolean insertUser(Connection conn, User user) throws SQLException {
        String queryInsertUser = "INSERT INTO utilizador (email, password, name, student_number, admin) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement insertUserStatement = conn.prepareStatement(queryInsertUser);
        insertUserStatement.setString(1, user.getEmail());
        insertUserStatement.setString(2, user.getPassword());
        insertUserStatement.setString(3, user.getName());
        insertUserStatement.setString(4, user.getStudentNumber());
        insertUserStatement.setBoolean(5, false);

        int rowsAffected = insertUserStatement.executeUpdate();

        return rowsAffected > 0;
    }

    public static boolean editUser(Connection conn, User user) throws SQLException {
        String queryUpdateUser = "UPDATE utilizador SET password = ?, name = ?, student_number = ? WHERE email = ?";

        PreparedStatement updateUserStatement = conn.prepareStatement(queryUpdateUser);
        updateUserStatement.setString(1, user.getPassword());
        updateUserStatement.setString(2, user.getName());
        updateUserStatement.setString(3, user.getStudentNumber());
        updateUserStatement.setString(4, user.getEmail());

        int rowsAffected = updateUserStatement.executeUpdate();

        return rowsAffected > 0;
    }
}
