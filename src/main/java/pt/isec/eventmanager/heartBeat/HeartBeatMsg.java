package pt.isec.eventmanager.heartBeat;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetAddress;

public class HeartBeatMsg implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int registryPort;
    private String serverRMIServiceName;
    private int dbVersion;
    private InetAddress ipAddress;

    public HeartBeatMsg(int registryPort, String serverRMIServiceName, int dbVersion, InetAddress ipAddress) {
        this.registryPort = registryPort;
        this.serverRMIServiceName = serverRMIServiceName;
        this.dbVersion = dbVersion;
        this.ipAddress = ipAddress;
    }

    public int getRegistryPort() {
        return registryPort;
    }

    public void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
    }

    public String getServerRMIServiceName() {
        return serverRMIServiceName;
    }

    public void setServerRMIServiceName(String serverRMIServiceName) {
        this.serverRMIServiceName = serverRMIServiceName;
    }

    public int getDbVersion() {
        return dbVersion;
    }

    public void setDbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }
}
