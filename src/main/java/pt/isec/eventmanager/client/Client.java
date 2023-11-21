package pt.isec.eventmanager.client;

import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.users.User;
import pt.isec.eventmanager.users.UserKey;
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

    private Socket socket;
    ObjectInputStream oin;
    ObjectOutputStream oout;

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
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.socket = socket;
            this.oin = oin;
            this.oout = oout;
            return null;
        } catch (Exception e) {
            return "Ocorreu um erro no acesso ao socket:\n\t" + e.getMessage();
        }
    }

    //USERS
    public boolean login(String username, String password) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

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
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            // Enviar o tipo de operação
            oout.writeObject(Constants.EDITUSER_REQUEST);
            oout.flush();

            oout.writeObject(user);
            oout.flush();

            try {
                // Receber resposta do servidor
                boolean success = (boolean) oin.readObject();

                if (success) {
                    this.user = user;
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

    @SuppressWarnings("unchecked")
    public ArrayList<Event> listUserEvents(String username) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            oout.writeObject(Constants.LISTUSEREVENTS_REQUEST);
            oout.flush();

            if (user.isAdmin())
                oout.writeObject(username);
            else
                oout.writeObject(user.getEmail());
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

    //EVENTS
    public boolean addEvent(Event event) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            oout.writeObject(Constants.INSERTEVENT_REQUEST);
            oout.flush();

            oout.writeObject(event);
            oout.flush();

            try {
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

    @SuppressWarnings("unchecked")
    public ArrayList<Event> listEvents() {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

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

    public boolean editEvent(Event event) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            oout.writeObject(Constants.EDITEVENT_REQUEST);
            oout.flush();

            oout.writeObject(event);
            oout.flush();

            try {
                boolean editSuccess = (boolean) oin.readObject();

                if (editSuccess) {
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

    public boolean deleteEvent(Event event) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            oout.writeObject(Constants.DELETEEVENT_REQUEST);
            oout.flush();

            oout.writeObject(event);
            oout.flush();

            try {
                boolean success = (boolean) oin.readObject();

                if (success) {
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

    public boolean checkEventHasAttendences(int eventId) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            oout.writeObject(Constants.EVENTHASATTENDENCES_REQUEST);
            oout.flush();

            oout.writeObject(eventId);
            oout.flush();

            try {
                boolean eventHasAttendences = (boolean) oin.readObject();

                if (eventHasAttendences) {
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

    public ArrayList<Attendance> listAttendences(int eventId) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            // Enviar o tipo de operação
            oout.writeObject(Constants.LISTATTENDENCES_REQUEST);
            oout.flush();

            oout.writeObject(eventId);
            oout.flush();

            try {
                return (ArrayList<Attendance>) oin.readObject();
            } catch (SocketTimeoutException e) {
                System.out.println("[Client] Socket timeout");
                return null;
            }

        } catch (Exception e) {
            System.out.println("[Client] Erro during socket creation :\n\t" + e.getMessage());
        }
        return null;
    }

    public boolean addAttendance(Attendance attendance) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            oout.writeObject(Constants.ADDATTENDENCE_REQUEST);
            oout.flush();

            oout.writeObject(attendance);
            oout.flush();

            try {
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

    public boolean deleteAttendance(Attendance attendance) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            oout.writeObject(Constants.DELETEATTENDENCE_REQUEST);
            oout.flush();

            oout.writeObject(attendance);
            oout.flush();

            try {
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

    //EVENT KEY
    public EventKey getEventKey(Event event) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            oout.writeObject(Constants.GETEVENTKEY_REQUEST);
            oout.flush();

            oout.writeObject(event.getId());
            oout.flush();

            try {
                return (EventKey) oin.readObject();
            } catch (SocketTimeoutException e) {
                System.out.println("[Client] Socket timeout");
            }
        } catch (Exception e) {
            System.out.println("[Client] Erro during socket creation :\n\t" + e.getMessage());
        }

        return null;
    }

    public boolean insertEventKey(EventKey eventKey) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            oout.writeObject(Constants.GENERATEEVENTKEY_REQUEST);
            oout.flush();

            oout.writeObject(eventKey);
            oout.flush();

            try {
                boolean generateKeySuccess = (boolean) oin.readObject();

                if (generateKeySuccess) {
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

    //USER KEY
    public boolean insertUserKey(UserKey userKey) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddress), Integer.parseInt(serverPort));
             ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(Constants.SERVER_TIMEOUT * 1000);

            oout.writeObject(Constants.INSERTUSERKEY_REQUEST);
            oout.flush();

            oout.writeObject(userKey);
            oout.flush();

            try {
                boolean insertUserKeySuccess = (boolean) oin.readObject();

                if (insertUserKeySuccess) {
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
}
