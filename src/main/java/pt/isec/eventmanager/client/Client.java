package pt.isec.eventmanager.client;

import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.users.User;
import pt.isec.eventmanager.users.UserKey;
import pt.isec.eventmanager.util.Constants;

import java.io.IOException;
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

    private Socket socket;
    private ObjectInputStream oin;
    private ObjectOutputStream oout;


    private ClientAuthenticatedController clientAuthenticatedController;
    private ClientThread clientThread;

    public Client(String serverAddress, String serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
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

    public void setClientAuthenticatedController(ClientAuthenticatedController controller) {
        this.clientAuthenticatedController = controller;
    }

    public String connect() {
        return null;
    }

    //USERS
    public boolean login(String username, String password) {
        try {
            Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
            ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());

            oout.writeObject(Constants.AUTHENTICATION_REQUEST);
            oout.flush();

            oout.writeObject(new User(username, password));
            oout.flush();

            try {
                User authenticatedUser = (User) oin.readObject();

                if (authenticatedUser != null) {
                    this.user = authenticatedUser;
                    this.socket = socket;
                    this.oin = oin;
                    this.oout = oout;

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
        this.serverAddress = "";
        this.serverPort = "";

        if (clientThread != null)
            clientThread.interrupt();

        if (this.socket != null)
            try {
                oin.close();
                oout.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("[Client] Error logging out: " + e.getMessage());
            }

        this.user = null;
    }

    public boolean registerUser(String email, String password, String name, String studentNumber) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

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

    public boolean editUser(User user) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            oout.writeObject(Constants.EDITUSER_REQUEST);
            oout.flush();

            oout.writeObject(user);
            oout.flush();

            boolean success = (boolean) oin.readObject();

            if (success) {
                this.user = user;
                return true;
            }

            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Event> listUserEvents(String username) {
        if (socket == null || oin == null || oout == null) return null;

        try {
            oout.writeObject(Constants.LISTUSEREVENTS_REQUEST);
            oout.flush();

            if (user.isAdmin())
                oout.writeObject(username);
            else
                oout.writeObject(user.getEmail());
            oout.flush();

            return (ArrayList<Event>) oin.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    //EVENTS
    public boolean addEvent(Event event) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            oout.writeObject(Constants.INSERTEVENT_REQUEST);
            oout.flush();

            oout.writeObject(event);
            oout.flush();

            boolean registrationSuccess = (boolean) oin.readObject();

            return registrationSuccess;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Event> listEvents() {
        if (socket == null || oin == null || oout == null) return null;

        try {
            oout.writeObject(Constants.LISTEVENTS_REQUEST);
            oout.flush();

            return (ArrayList<Event>) oin.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public boolean editEvent(Event event) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            oout.writeObject(Constants.EDITEVENT_REQUEST);
            oout.flush();

            oout.writeObject(event);
            oout.flush();

            boolean success = (boolean) oin.readObject();

            return success;

        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public boolean deleteEvent(Event event) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            oout.writeObject(Constants.DELETEEVENT_REQUEST);
            oout.flush();

            oout.writeObject(event);
            oout.flush();

            boolean success = (boolean) oin.readObject();

            return success;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public boolean checkEventHasAttendences(int eventId) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            oout.writeObject(Constants.EVENTHASATTENDENCES_REQUEST);
            oout.flush();

            oout.writeObject(eventId);
            oout.flush();

            boolean success = (boolean) oin.readObject();

            return success;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Attendance> listAttendences(int eventId) {
        if (socket == null || oin == null || oout == null) return null;

        try {
            oout.writeObject(Constants.LISTATTENDENCES_REQUEST);
            oout.flush();

            oout.writeObject(eventId);
            oout.flush();

            return (ArrayList<Attendance>) oin.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public boolean addAttendance(Attendance attendance) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            oout.writeObject(Constants.ADDATTENDENCE_REQUEST);
            oout.flush();

            oout.writeObject(attendance);
            oout.flush();

            boolean success = (boolean) oin.readObject();

            return success;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public boolean deleteAttendance(Attendance attendance) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            oout.writeObject(Constants.DELETEATTENDENCE_REQUEST);
            oout.flush();

            oout.writeObject(attendance);
            oout.flush();

            boolean success = (boolean) oin.readObject();

            return success;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    //EVENT KEY
    public EventKey getEventKey(Event event) {
        if (socket == null || oin == null || oout == null) return null;

        try {
            oout.writeObject(Constants.GETEVENTKEY_REQUEST);
            oout.flush();

            oout.writeObject(event.getId());
            oout.flush();

            return (EventKey) oin.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public boolean insertEventKey(EventKey eventKey) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            oout.writeObject(Constants.GENERATEEVENTKEY_REQUEST);
            oout.flush();

            oout.writeObject(eventKey);
            oout.flush();

            boolean success = (boolean) oin.readObject();

            return success;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    //USER KEY
    public boolean insertUserKey(UserKey userKey) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            oout.writeObject(Constants.INSERTUSERKEY_REQUEST);
            oout.flush();

            oout.writeObject(userKey);
            oout.flush();

            boolean success = (boolean) oin.readObject();

            return success;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }
}
