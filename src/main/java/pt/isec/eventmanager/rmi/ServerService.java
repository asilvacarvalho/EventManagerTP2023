package pt.isec.eventmanager.rmi;

import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.server.ServerController;
import pt.isec.eventmanager.users.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServerService extends UnicastRemoteObject implements ServerServiceInterface {
    public static final int MAX_CHUNCK_SIZE = 10000;

    private final File dbDirectory;
    private String dbFileName;
    private ServerController serverController;

    private boolean isGetDBFileRunning = false;

    private final List<ServerServiceObserverInterface> observers;

    public ServerService(File dbDirectory, String dbFileName, ServerController controller) throws RemoteException {
        this.dbDirectory = dbDirectory;
        this.dbFileName = dbFileName;
        this.serverController = controller;

        observers = new ArrayList<>();
    }

    public boolean isGetDBFileRunning() {
        return isGetDBFileRunning;
    }

    private FileInputStream getRequestedFileInputStream() throws IOException {
        String requestedCanonicalFilePath;

        dbFileName = dbFileName.trim();

        requestedCanonicalFilePath = new File(dbDirectory + File.separator + dbFileName).getCanonicalPath();

        if (!requestedCanonicalFilePath.startsWith(dbDirectory.getCanonicalPath() + File.separator)) {
            System.out.println("Nao é permitido aceder ao ficheiro " + requestedCanonicalFilePath + "!");
            System.out.println("A directoria de base nao corresponde a " + dbDirectory.getCanonicalPath() + "!");
            throw new AccessDeniedException(dbFileName);
        }

        return new FileInputStream(requestedCanonicalFilePath);
    }

    @Override
    public void getDBFile(BackupServerServiceInterface backupServRef) throws IOException {
        isGetDBFileRunning = true;
        byte[] fileChunk = new byte[MAX_CHUNCK_SIZE];
        int nbytes;

        try (FileInputStream requestedFileInputStream = getRequestedFileInputStream()) {

            while ((nbytes = requestedFileInputStream.read(fileChunk)) != -1) {
                backupServRef.writeFileChunk(fileChunk, nbytes);
            }

            System.out.println("[ServerService] Ficheiro da BDtransferido para com sucesso.");
            serverController.addToConsole("[ServerService] Ficheiro da BDtransferido para com sucesso.");
            notifyObservers("Ficheiro da BDtransferido para com sucesso.");

            isGetDBFileRunning = false;

        } catch (FileNotFoundException e) {
            System.out.println("[ServerService] Ocorreu a excecao {" + e + "} ao tentar abrir o ficheiro!");
            notifyObservers("[ServerService] Ocorreu a excecao {" + e + "} ao tentar abrir o ficheiro!");
            serverController.addToHeartBeatConsole("[ServerService] Ocorreu a excecao {" + e + "} ao tentar abrir o ficheiro!");
            isGetDBFileRunning = false;
            throw new FileNotFoundException(dbFileName);
        } catch (IOException e) {
            System.out.println("Ocorreu a excecao de E/S: \n\t" + e.getMessage());
            notifyObservers("Ocorreu a excecao de E/S: \n\t" + e.getMessage());
            serverController.addToConsole("Ocorreu a excecao de E/S: \n\t" + e.getMessage());
            isGetDBFileRunning = false;
            throw new IOException(dbFileName, e.getCause());
        }
    }

    public void addObserver(ServerServiceObserverInterface observer) throws RemoteException {
        synchronized (observers) {
            if (!observers.contains(observer)) {
                observers.add(observer);
                System.out.println("[ServerService] Observer Added");
                serverController.addToConsole("[ServerService] Observer Added");
            }
            serverController.initObserversListView(observers);
        }
    }

    public void removeObserver(ServerServiceObserverInterface observer) throws RemoteException {
        synchronized (observers) {
            if (observers.remove(observer)) {
                System.out.println("[ServerService] Observer Removed");
                serverController.addToConsole("[ServerService] Observer Removed");
            }
            serverController.initObserversListView(observers);
        }
    }

    public void notifyObservers(String msg) {
        List<ServerServiceObserverInterface> observersToRemove = new ArrayList<>();

        for (ServerServiceObserverInterface observer : observers) {
            try {
                observer.notifyOperationConcluded(msg);
            } catch (RemoteException e) {
                observersToRemove.add(observer);
                System.out.println("[ServerService] - um observador (observador inacessivel).");
                serverController.addToConsole("[ServerService] - um observador (observador inacessivel).");
            }
        }
        synchronized (observers) {
            observers.removeAll(observersToRemove);
        }
    }

    public List<ServerServiceObserverInterface> getObservers() {
        return observers;
    }

    public void inserUser(int dbVersion, User user) {
        List<ServerServiceObserverInterface> observersToRemove = new ArrayList<>();

        for (ServerServiceObserverInterface observer : observers) {
            try {
                observer.insertUser(dbVersion, user);
            } catch (RemoteException | SQLException e) {
                observersToRemove.add(observer);
                System.out.println("[ServerService] - um observador (observador inacessivel).");
                serverController.addToConsole("[ServerService] - um observador (observador inacessivel).");
            }
        }
        synchronized (observers) {
            observers.removeAll(observersToRemove);
        }
    }

    public void editUser(int dbVersion, User user) {
        List<ServerServiceObserverInterface> observersToRemove = new ArrayList<>();

        for (ServerServiceObserverInterface observer : observers) {
            try {
                observer.editUser(dbVersion, user);
            } catch (RemoteException | SQLException e) {
                observersToRemove.add(observer);
                System.out.println("[ServerService] - um observador (observador inacessivel).");
                serverController.addToConsole("[ServerService] - um observador (observador inacessivel).");
            }
        }
        synchronized (observers) {
            observers.removeAll(observersToRemove);
        }
    }

    public void inserEvent(int dbVersion, Event event) {
        List<ServerServiceObserverInterface> observersToRemove = new ArrayList<>();

        for (ServerServiceObserverInterface observer : observers) {
            try {
                observer.insertEvent(dbVersion, event);
            } catch (RemoteException | SQLException e) {
                observersToRemove.add(observer);
                System.out.println("[ServerService] - um observador (observador inacessivel).");
                serverController.addToConsole("[ServerService] - um observador (observador inacessivel).");
            }
        }
        synchronized (observers) {
            observers.removeAll(observersToRemove);
        }
    }

    public void deleteEvent(int dbVersion, Event event) {
        List<ServerServiceObserverInterface> observersToRemove = new ArrayList<>();

        for (ServerServiceObserverInterface observer : observers) {
            try {
                observer.deleteEvent(dbVersion, event);
            } catch (RemoteException | SQLException e) {
                observersToRemove.add(observer);
                System.out.println("[ServerService] - um observador (observador inacessivel).");
                serverController.addToConsole("[ServerService] - um observador (observador inacessivel).");
            }
        }
        synchronized (observers) {
            observers.removeAll(observersToRemove);
        }
    }

    public void editEvent(int dbVersion, Event event) {
        List<ServerServiceObserverInterface> observersToRemove = new ArrayList<>();

        for (ServerServiceObserverInterface observer : observers) {
            try {
                observer.editEvent(dbVersion, event);
            } catch (RemoteException e) {
                observersToRemove.add(observer);
                System.out.println("[ServerService] - um observador (observador inacessivel).");
                serverController.addToConsole("[ServerService] - um observador (observador inacessivel).");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        synchronized (observers) {
            observers.removeAll(observersToRemove);
        }
    }

    public void inserAttendance(int dbVersion, int eventId, String username) {
        List<ServerServiceObserverInterface> observersToRemove = new ArrayList<>();

        for (ServerServiceObserverInterface observer : observers) {
            try {
                observer.insertAttendance(dbVersion, eventId, username);
            } catch (RemoteException | SQLException e) {
                observersToRemove.add(observer);
                System.out.println("[ServerService] - um observador (observador inacessivel).");
                serverController.addToConsole("[ServerService] - um observador (observador inacessivel).");
            }
        }
        synchronized (observers) {
            observers.removeAll(observersToRemove);
        }
    }

    public void deleteAttendance(int dbVersion, int eventId, String username) {
        List<ServerServiceObserverInterface> observersToRemove = new ArrayList<>();

        for (ServerServiceObserverInterface observer : observers) {
            try {
                observer.deleteAttendance(dbVersion, eventId, username);
            } catch (RemoteException | SQLException e) {
                observersToRemove.add(observer);
                System.out.println("[ServerService] - um observador (observador inacessivel).");
                serverController.addToConsole("[ServerService] - um observador (observador inacessivel).");
            }
        }
        synchronized (observers) {
            observers.removeAll(observersToRemove);
        }
    }

    public void inserEventKey(int dbVersion, EventKey eventKey) {
        List<ServerServiceObserverInterface> observersToRemove = new ArrayList<>();

        for (ServerServiceObserverInterface observer : observers) {
            try {
                observer.insertEventKey(dbVersion, eventKey);
            } catch (RemoteException | SQLException e) {
                observersToRemove.add(observer);
                System.out.println("[ServerService] - um observador (observador inacessivel).");
                serverController.addToConsole("[ServerService] - um observador (observador inacessivel).");
            }
        }
        synchronized (observers) {
            observers.removeAll(observersToRemove);
        }
    }

    public void deleteEventKey(int dbVersion, EventKey eventKey) {
        List<ServerServiceObserverInterface> observersToRemove = new ArrayList<>();

        for (ServerServiceObserverInterface observer : observers) {
            try {
                observer.deleteEventKey(dbVersion, eventKey);
            } catch (RemoteException | SQLException e) {
                observersToRemove.add(observer);
                System.out.println("[ServerService] - um observador (observador inacessivel).");
                serverController.addToConsole("[ServerService] - um observador (observador inacessivel).");
            }
        }
        synchronized (observers) {
            observers.removeAll(observersToRemove);
        }
    }
}
