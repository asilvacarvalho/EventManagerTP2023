package pt.isec.eventmanager.events;


import java.io.Serial;
import java.io.Serializable;

public class Attendance implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int eventId;
    private String username;

    public Attendance() {
    }

    public Attendance(int eventId, String username) {
        this.eventId = eventId;
        this.username = username;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
