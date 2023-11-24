package pt.isec.eventmanager.server;

import java.io.Serial;
import java.io.Serializable;

public class ServerOperation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String operation;

    public ServerOperation(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
