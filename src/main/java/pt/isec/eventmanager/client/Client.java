package pt.isec.eventmanager.client;

import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.users.User;
import pt.isec.eventmanager.util.Constants;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class Client {
    private User user;
    private String serverAddress;
    private String serverPort;

    public Client() {
    }

    public User getUser() {
        return user;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getServerPort() {
        return serverPort;
    }

    public String connect(String serverAddress, String serverPort) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort))) {
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            return null;
        } catch (Exception e) {
            return "Ocorreu um erro no acesso ao socket:\n\t" + e.getMessage();
        }
    }

    public boolean login(String username, String password) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.TIMEOUT * 1000);

            // Enviar o tipo de operação
            oout.writeObject(Constants.AUTHENTICATION_REQUEST);
            oout.flush();

            // Enviar dados de autenticação
            oout.writeObject(new User(username, password));
            oout.flush();

            try {
                // Receber resposta do servidor
                User authenticatedUser = (User) oin.readObject();

                if (authenticatedUser != null) {
                    this.user = authenticatedUser;
                    return true;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("[Client] Socket timeout");
                return false;
            }

        } catch (Exception e) {
            System.out.println("[Client] Erro during socket creation :\n\t" + e.getMessage());
        }
        return false;
    }

    public void logout() {
        this.user = null;
    }

    public boolean registerUser(String email, String password, String name, String studentNumber) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.TIMEOUT * 1000);

            // Enviar o tipo de operação
            oout.writeObject(Constants.INSERTUSER_REQUEST);
            oout.flush();

            oout.writeObject(new User(email, password, name, studentNumber, false));
            oout.flush();

            try {
                // Receber resposta do servidor
                boolean registrationSuccess = (boolean) oin.readObject();

                if (registrationSuccess) {
                    return true;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("[Client] Socket timeout");
                return false;
            }

        } catch (Exception e) {
            System.out.println("[Client] Erro during socket creation :\n\t" + e.getMessage());
        }
        return false;
    }

    public boolean addEvent(Event event) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.TIMEOUT * 1000);

            // Enviar o tipo de operação
            oout.writeObject(Constants.INSERTEVENT_REQUEST);
            oout.flush();

            oout.writeObject(event);
            oout.flush();

            try {
                // Receber resposta do servidor
                boolean registrationSuccess = (boolean) oin.readObject();

                if (registrationSuccess) {
                    return true;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("[Client] Socket timeout");
                return false;
            }

        } catch (Exception e) {
            System.out.println("[Client] Erro during socket creation :\n\t" + e.getMessage());
        }
        return false;
    }

    public ArrayList<Event> listEvents() {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.TIMEOUT * 1000);

            // Enviar o tipo de operação
            oout.writeObject(Constants.LISTEVENTS_REQUEST);
            oout.flush();

            try {
                return (ArrayList<Event>) oin.readObject();
            } catch (SocketTimeoutException e) {
                System.out.println("[Client] Socket timeout");
                return null;
            }

        } catch (Exception e) {
            System.out.println("[Client] Erro during socket creation :\n\t" + e.getMessage());
        }
        return null;
    }
}
