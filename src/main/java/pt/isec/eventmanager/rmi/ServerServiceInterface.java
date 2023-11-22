package pt.isec.eventmanager.rmi;

import java.io.IOException;

public interface ServerServiceInterface extends java.rmi.Remote {
    void getDBFile(BackupServerServiceInterface cliRef) throws IOException;
}
