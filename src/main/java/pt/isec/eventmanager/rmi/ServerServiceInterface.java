package pt.isec.eventmanager.rmi;

import java.io.IOException;
import java.rmi.RemoteException;

public interface ServerServiceInterface extends java.rmi.Remote {
    void getDBFile(BackupServerServiceInterface cliRef) throws IOException;

    void addObserver(ServerServiceObserverInterface observer) throws RemoteException;

    void removeObserver(ServerServiceObserverInterface observer) throws RemoteException;
}
