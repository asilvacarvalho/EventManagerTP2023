package pt.isec.eventmanager.client;

import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.server.ServerOperation;
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
        try {
            Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
            ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());

            startListeningforRefresh(socket, oin, oout);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void startListeningforRefresh(Socket socket, ObjectInputStream oin, ObjectOutputStream oout) {
        clientThread = new ClientThread(socket, oin, oout, this);
        clientThread.setDaemon(true);
        clientThread.start();
    }

    public void refreshClientEvents() {
        clientAuthenticatedController.refreshListEvens();
    }

    public void refreshClientAttendances(int eventId) {
        clientAuthenticatedController.refreshListEventAttendances(eventId);
    }

    //USERS
    public boolean login(String username, String password) {
        try {
            Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
            ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());

            try {
                try {
                    oout.writeObject("DATABASE_OPERATIONS");
                    oout.flush();
                } catch (IOException e) {
                    return false;
                }

                ServerOperation operation = new ServerOperation(Constants.AUTHENTICATION_REQUEST);
                operation.setUser(new User(username, password));

                oout.writeObject(operation);
                oout.flush();

                ServerOperation response = (ServerOperation) oin.readObject();

                if (response.getOperation().equals(Constants.AUTHENTICATION_REQUEST) && response.getUser() != null) {
                    this.user = response.getUser();
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

            ServerOperation operation = new ServerOperation(Constants.INSERTUSER_REQUEST);
            operation.setUser(new User(email, password, name, studentNumber, false));
            oout.writeObject(operation);
            oout.flush();

            try {
                ServerOperation response = (ServerOperation) oin.readObject();

                if (response.getOperation().equals(Constants.INSERTUSER_REQUEST) && response.getResult()) {
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
            ServerOperation operation = new ServerOperation(Constants.EDITUSER_REQUEST);
            operation.setUser(user);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.EDITUSER_REQUEST) && response.getResult()) {
                this.user = user;
                return true;
            }

            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public ArrayList<Event> listUserEvents(String username) {
        if (socket == null || oin == null || oout == null) return null;

        try {
            ServerOperation operation = new ServerOperation(Constants.LISTUSEREVENTS_REQUEST);

            if (user.isAdmin())
                operation.setUserName(username);
            else
                operation.setUserName(user.getEmail());

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.LISTUSEREVENTS_REQUEST) && response.getListEvents() != null) {
                return response.getListEvents();
            }

            return null;

        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    //EVENTS
    public boolean addEvent(Event event) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            ServerOperation operation = new ServerOperation(Constants.INSERTEVENT_REQUEST);
            operation.setEvent(event);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.INSERTEVENT_REQUEST))
                return response.getResult();

            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public ArrayList<Event> listEvents() {
        if (socket == null || oin == null || oout == null) return null;

        try {
            ServerOperation operation = new ServerOperation(Constants.LISTEVENTS_REQUEST);
            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.LISTEVENTS_REQUEST) && response.getListEvents() != null) {
                return response.getListEvents();
            }

            return null;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public boolean editEvent(Event event) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            ServerOperation operation = new ServerOperation(Constants.EDITEVENT_REQUEST);
            operation.setEvent(event);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.EDITEVENT_REQUEST))
                return response.getResult();

            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public boolean deleteEvent(Event event) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            ServerOperation operation = new ServerOperation(Constants.DELETEEVENT_REQUEST);
            operation.setEvent(event);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.DELETEEVENT_REQUEST))
                return response.getResult();

            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public boolean checkEventHasAttendences(int eventId) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            ServerOperation operation = new ServerOperation(Constants.EVENTHASATTENDENCES_REQUEST);
            operation.setEventId(eventId);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.EVENTHASATTENDENCES_REQUEST))
                return response.getResult();

            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public ArrayList<Attendance> listAttendences(int eventId) {
        if (socket == null || oin == null || oout == null) return null;

        try {
            ServerOperation operation = new ServerOperation(Constants.LISTATTENDENCES_REQUEST);
            operation.setEventId(eventId);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.LISTATTENDENCES_REQUEST) && response.getListAttendances() != null) {
                return response.getListAttendances();
            }

            return null;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public boolean addAttendance(Attendance attendance) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            ServerOperation operation = new ServerOperation(Constants.ADDATTENDENCE_REQUEST);
            operation.setAttendance(attendance);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.ADDATTENDENCE_REQUEST))
                return response.getResult();

            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public boolean deleteAttendance(Attendance attendance) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            ServerOperation operation = new ServerOperation(Constants.DELETEATTENDENCE_REQUEST);
            operation.setAttendance(attendance);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.DELETEATTENDENCE_REQUEST))
                return response.getResult();

            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    //EVENT KEY
    public EventKey getEventKey(Event event) {
        if (socket == null || oin == null || oout == null) return null;

        try {
            ServerOperation operation = new ServerOperation(Constants.GETEVENTKEY_REQUEST);
            operation.setEvent(event);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.GETEVENTKEY_REQUEST) && response.getEventKey() != null)
                return response.getEventKey();

            return null;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public boolean insertEventKey(EventKey eventKey) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            ServerOperation operation = new ServerOperation(Constants.INSERTEVENTKEY_REQUEST);
            operation.setEventKey(eventKey);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.INSERTEVENTKEY_REQUEST))
                return response.getResult();

            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    //USER KEY
    public boolean insertUserKey(UserKey userKey) {
        if (socket == null || oin == null || oout == null) return false;

        try {
            ServerOperation operation = new ServerOperation(Constants.INSERTUSERKEY_REQUEST);
            operation.setUserKey(userKey);

            oout.writeObject(operation);
            oout.flush();

            ServerOperation response = (ServerOperation) oin.readObject();

            if (response.getOperation().equals(Constants.INSERTUSERKEY_REQUEST))
                return response.getResult();

            return false;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }
}
