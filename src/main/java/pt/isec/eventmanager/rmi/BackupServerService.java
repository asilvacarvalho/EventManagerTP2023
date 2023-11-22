package pt.isec.eventmanager.rmi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class BackupServerService extends UnicastRemoteObject implements BackupServerServiceInterface {
    FileOutputStream fout = null;

    public BackupServerService() throws RemoteException {
    }

    public synchronized void setFout(FileOutputStream fout) {
        this.fout = fout;
    }

    @Override
    public void writeFileChunk(byte[] fileChunk, int nbytes) throws IOException {
        if (fout == null) {
            System.out.println("[BackupServerService] Não existe obejto aberto para escrita");
            throw new IOException();
        }

        try {
            fout.write(fileChunk, 0, nbytes);
        } catch (IOException e) {
            System.out.println("[BackupServerService] Exception ao escrever o ficheiro: " + e.getMessage());
            throw new IOException();
        }
    }
}
