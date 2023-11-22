package pt.isec.eventmanager.rmi;

public interface BackupServerServiceInterface extends java.rmi.Remote {
    void writeFileChunk(byte[] fileChunk, int nbytes) throws java.io.IOException;
}
