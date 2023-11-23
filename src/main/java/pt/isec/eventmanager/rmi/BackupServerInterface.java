package pt.isec.eventmanager.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BackupServerInterface extends Remote {
    void registerForUpdate(String backupServerName) throws RemoteException;
}
