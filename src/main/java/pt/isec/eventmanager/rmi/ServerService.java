package pt.isec.eventmanager.rmi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ServerService extends UnicastRemoteObject implements ServerServiceInterface {
    public static final int MAX_CHUNCK_SIZE = 10000;

    private final File dbDirectory;
    private String dbFileName;

    public ServerService(File dbDirectory, String dbFileName) throws RemoteException {
        this.dbDirectory = dbDirectory;
        this.dbFileName = dbFileName;
    }

    private FileInputStream getRequestedFileInputStream() throws IOException {
        String requestedCanonicalFilePath;

        dbFileName = dbFileName.trim();

        requestedCanonicalFilePath = new File(dbDirectory + File.separator + dbFileName).getCanonicalPath();

        if (!requestedCanonicalFilePath.startsWith(dbDirectory.getCanonicalPath() + File.separator)) {
            System.out.println("Nao e' permitido aceder ao ficheiro " + requestedCanonicalFilePath + "!");
            System.out.println("A directoria de base nao corresponde a " + dbDirectory.getCanonicalPath() + "!");
            throw new AccessDeniedException(dbFileName);
        }

        return new FileInputStream(requestedCanonicalFilePath);
    }

    @Override
    public void getDBFile(BackupServerServiceInterface backupServRef) throws IOException {
        byte[] fileChunk = new byte[MAX_CHUNCK_SIZE];
        int nbytes;

        try (FileInputStream requestedFileInputStream = getRequestedFileInputStream()) {

            while ((nbytes = requestedFileInputStream.read(fileChunk)) != -1) {
                backupServRef.writeFileChunk(fileChunk, nbytes);
            }

            System.out.println("Ficheiro " + new File(dbDirectory + File.separator + dbFileName).getCanonicalPath() +
                    " transferido para o cliente com sucesso.");
            System.out.println();
            //notifyObservers("Ficheiro Transferido");

        } catch (FileNotFoundException e) {
            System.out.println("Ocorreu a excecao {" + e + "} ao tentar abrir o ficheiro!");
            throw new FileNotFoundException(dbFileName);
        } catch (IOException e) {
            System.out.println("Ocorreu a excecao de E/S: \n\t" + e);
            throw new IOException(dbFileName, e.getCause());
        }
    }
}
