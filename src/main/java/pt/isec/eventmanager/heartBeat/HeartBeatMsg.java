package pt.isec.eventmanager.heartBeat;

import java.io.Serial;
import java.io.Serializable;

public class HeartBeatMsg implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int registryPort;
    private String serverRMIServiceName;
    private int dbVersion;

    public HeartBeatMsg(int registryPort, String serverRMIServiceName, int dbVersion) {
        this.registryPort = registryPort;
        this.serverRMIServiceName = serverRMIServiceName;
        this.dbVersion = dbVersion;
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
}
