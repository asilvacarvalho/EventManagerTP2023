package pt.isec.eventmanager.rmi;

import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.serverBackup.ServerBackup;
import pt.isec.eventmanager.users.User;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;

public class ServerServiceObserver extends UnicastRemoteObject implements ServerServiceObserverInterface {
    private ServerBackup serverBackup;

    public ServerServiceObserver(ServerBackup serverBackup) throws RemoteException {
        this.serverBackup = serverBackup;
    }

    @Override
    public void notifyOperationConcluded(String description) throws RemoteException {
        if (serverBackup != null)
            serverBackup.observerNotify(description);
    }

    @Override
    public void insertUser(int dbVersion, User user) throws SQLException {
        if (serverBackup != null)
            serverBackup.insertUser(dbVersion, user);
    }

    @Override
    public void editUser(int dbVersion, User user) throws SQLException {
        if (serverBackup != null)
            serverBackup.editUser(dbVersion, user);
    }

    @Override
    public void insertEvent(int dbVersion, Event event) throws SQLException {
        if (serverBackup != null)
            serverBackup.insertEvent(dbVersion, event);
    }

    @Override
    public void deleteEvent(int dbVersion, Event event) throws SQLException {
        if (serverBackup != null)
            serverBackup.deleteEvent(dbVersion, event);
    }

    @Override
    public void editEvent(int dbVersion, Event event) throws SQLException {
        if (serverBackup != null)
            serverBackup.editEvent(dbVersion, event);
    }

    @Override
    public void insertAttendance(int dbVersion, int eventId, String username) throws SQLException {
        if (serverBackup != null)
            serverBackup.insertAttendance(dbVersion, eventId, username);
    }

    @Override
    public void deleteAttendance(int dbVersion, int eventId, String username) throws SQLException {
        if (serverBackup != null)
            serverBackup.deleteAttendance(dbVersion, eventId, username);
    }

    @Override
    public void insertEventKey(int dbVersion, EventKey eventKey) throws SQLException {
        if (serverBackup != null)
            serverBackup.insertEventKey(dbVersion, eventKey);
    }

    @Override
    public void deleteEventKey(int dbVersion, EventKey eventKey) throws SQLException {
        if (serverBackup != null)
            serverBackup.deleteEventKey(dbVersion, eventKey);
    }

}
