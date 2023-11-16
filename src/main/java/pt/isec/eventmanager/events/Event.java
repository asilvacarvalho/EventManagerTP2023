package pt.isec.eventmanager.events;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class Event implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String location;
    private Date date;
    private String startTime;
    private String endTime;

    public Event(String name, String location, Date date, String startTime, String endTime) {
        this.name = name;
        this.location = location;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
