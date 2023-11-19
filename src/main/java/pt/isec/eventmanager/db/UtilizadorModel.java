package pt.isec.eventmanager.db;

import pt.isec.eventmanager.server.ServerController;
import pt.isec.eventmanager.users.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UtilizadorModel {
    public static User authenticateUser(Connection conn, User user, ServerController controller) {
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
            controller.addToConsole("[EventManagerDB] Error in authenticateUser: " + e.getMessage());
        }
        return null;
    }

    public static boolean insertUser(Connection conn, User user, ServerController controller) {
        String queryCheckEmail = "SELECT COUNT(*) FROM utilizador WHERE email = ?";
        String queryInsertUser = "INSERT INTO utilizador (email, password, name, student_number, admin) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement checkEmailStatement = conn.prepareStatement(queryCheckEmail);
             PreparedStatement insertUserStatement = conn.prepareStatement(queryInsertUser)) {

            // Verifica se o email já existe na tabela
            checkEmailStatement.setString(1, user.getEmail());
            ResultSet resultSet = checkEmailStatement.executeQuery();
            if (resultSet.getInt(1) > 0) {
                System.err.println("[EventManagerDB] Error inserting user, email already exists");
                controller.addToConsole("[EventManagerDB] Error inserting user, email already exists");
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
            controller.addToConsole("[EventManagerDB] Error inserting user: " + e.getMessage());
        }

        return false;
    }

    public static User getUser(Connection conn, String username, ServerController controller) {
        String query = "SELECT * FROM utilizador WHERE email=?";
        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
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
        } catch (SQLException e) {
            System.err.println("[EventManagerDB] Error getting User: " + e.getMessage());
            controller.addToConsole("[EventManagerDB] Error getting User: " + e.getMessage());
        }
        return null;
    }
}
