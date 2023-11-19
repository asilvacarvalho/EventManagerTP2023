package pt.isec.eventmanager.events;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Random;

public class EventKey implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private Integer code;
    private Date endDate;
    private int eventId;

    public EventKey() {
    }

    public EventKey(int eventId) {
        this.eventId = eventId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public void generateCode() {
        Random random = new Random();
        this.code = 100000 + random.nextInt(900000);
    }
}
