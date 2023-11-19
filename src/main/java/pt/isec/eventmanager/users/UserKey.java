package pt.isec.eventmanager.users;

import java.io.Serial;
import java.io.Serializable;

public class UserKey implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String username;
    private int userKey;

    public UserKey(String username, int userKey) {
        this.username = username;
        this.userKey = userKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getUserKey() {
        return userKey;
    }

    public void setUserKey(int userKey) {
        this.userKey = userKey;
    }
}
