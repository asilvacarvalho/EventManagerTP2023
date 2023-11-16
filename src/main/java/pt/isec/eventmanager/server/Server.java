package pt.isec.eventmanager.server;

import pt.isec.eventmanager.db.EventManagerDB;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Server {
    private ServerController serverController;
    private String clientTcpPort;
    private String dbUrl;

    private int threadNumber;

    public Server() {
        this.threadNumber = 0;
    }

    public void initServer(String clientTcpPort, String dbUrl, ServerController controller) {
        this.serverController = controller;
        this.clientTcpPort = clientTcpPort;
        this.dbUrl = dbUrl;
        initDB();
        initTcpServer();
    }

    private void initDB() {
        File dbFile = new File(dbUrl);

        if (dbFile.exists()) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[Server] Connection Established to " + dbUrl);
                serverController.addToConsole("[Server] Connection Established to " + dbUrl);
            } catch (SQLException e) {
                System.err.println("[Server] Connection Error: " + e.getMessage());
                serverController.addToConsole("[Server] Connection Error: " + e.getMessage());
            }
        } else {
            System.err.println("[Server] Database does not exist. Creating tables and admin user.");

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbUrl)) {
                System.out.println("[Server] Connection Established to " + dbUrl);
                serverController.addToConsole("[Server] Connection Established to " + dbUrl);

                // Criação de tabelas e admin
                EventManagerDB.createTables(conn);
                EventManagerDB.createAdmin(conn);

            } catch (SQLException e) {
                System.err.println("[Server] Connection Error: " + e.getMessage());
                serverController.addToConsole("[Server] Connection Error: " + e.getMessage());
            }
        }
    }


    private void initTcpServer() {
        try (ServerSocket socket = new ServerSocket(Integer.parseInt(clientTcpPort))) {

            System.out.println("[Server] TCP Time Server inicialized in port " + socket.getLocalPort() + " ...");
            serverController.addToConsole("[Server] TCP Time Server inicialized in port " + socket.getLocalPort() + " ...");

            while (true) {
                try {
                    Socket toClientSocket = socket.accept();
                    Thread t = new ServerThread(toClientSocket, threadNumber++, dbUrl, serverController);

                    t.start();
                } catch (Exception e) {
                    System.out.println("[Server] Problem communication with client: " + e.getMessage());
                    serverController.addToConsole("[Server] Problem communication with client: " + e.getMessage());
                }
            }

        } catch (NumberFormatException e) {
            System.out.println("[Server] Port number must be a positive integer!");
            serverController.addToConsole("[Server] Port number must be a positive integer!");
        } catch (IOException e) {
            System.out.println("[Server] Error on the socket: " + e.getMessage());
            serverController.addToConsole("[Server] Error on the socket: " + e.getMessage());
        }
    }
}
