package pt.isec.eventmanager.rmi;

import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.users.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

public interface ServerServiceObserverInterface extends Remote {
    void notifyOperationConcluded(String description) throws RemoteException;

    void insertUser(int dbVersion, User user) throws RemoteException, SQLException;

    void editUser(int dbVersion, User user) throws RemoteException, SQLException;

    void insertEvent(int dbVersion, Event event) throws RemoteException, SQLException;

    void deleteEvent(int dbVersion, Event event) throws RemoteException, SQLException;

    void editEvent(int dbVersion, Event event) throws RemoteException, SQLException;

    void insertAttendance(int dbVersion, int eventId, String username) throws RemoteException, SQLException;

    void deleteAttendance(int dbVersion, int eventId, String username) throws RemoteException, SQLException;

    void insertEventKey(int dbVersion, EventKey eventKey) throws RemoteException, SQLException;

    void deleteEventKey(int dbVersion, EventKey eventKey) throws RemoteException, SQLException;
}
