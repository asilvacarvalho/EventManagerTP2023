package pt.isec.eventmanager.server;

import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.users.User;
import pt.isec.eventmanager.users.UserKey;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class ServerOperation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String operation;
    private String userName;

    private int eventId;

    private boolean result;

    private ArrayList<Event> listEvents;
    private ArrayList<Attendance> listAttendances;

    private User user;
    private Event event;
    private Attendance attendance;
    private EventKey eventKey;
    private UserKey userKey;

    public ServerOperation(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public ArrayList<Event> getListEvents() {
        return listEvents;
    }

    public void setListEvents(ArrayList<Event> listEvents) {
        this.listEvents = listEvents;
    }

    public ArrayList<Attendance> getListAttendances() {
        return listAttendances;
    }

    public void setListAttendances(ArrayList<Attendance> listAttendances) {
        this.listAttendances = listAttendances;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public EventKey getEventKey() {
        return eventKey;
    }

    public void setEventKey(EventKey eventKey) {
        this.eventKey = eventKey;
    }

    public UserKey getUserKey() {
        return userKey;
    }

    public void setUserKey(UserKey userKey) {
        this.userKey = userKey;
    }
}
