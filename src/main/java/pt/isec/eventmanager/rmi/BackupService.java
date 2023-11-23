package pt.isec.eventmanager.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class BackupService extends UnicastRemoteObject implements BackupServerInterface {
    private List<String> registeredServers;

    protected BackupService() throws RemoteException {
        registeredServers = new ArrayList<>();
    }

    @Override
    public void registerForUpdate(String backupServerName) throws RemoteException {
        registeredServers.add(backupServerName);
        System.out.println("Backup server registered: " + backupServerName);
    }
}
