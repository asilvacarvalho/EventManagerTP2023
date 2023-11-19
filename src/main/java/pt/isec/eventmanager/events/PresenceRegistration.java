package pt.isec.eventmanager.events;


import pt.isec.eventmanager.users.User;

import java.io.Serial;
import java.io.Serializable;

public class PresenceRegistration implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private User user;
    private Event event;

    public PresenceRegistration() {
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
