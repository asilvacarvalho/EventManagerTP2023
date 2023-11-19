package pt.isec.eventmanager.events;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    public boolean isEventInProgress() {
        // Obtém a data e hora atuais
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Converte a data do evento para LocalDateTime considerando a zona de tempo padrão
        LocalDateTime eventDate = date
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // Converte as strings de hora do evento para LocalTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime startTime = LocalTime.parse(this.startTime, formatter);
        LocalTime endTime = LocalTime.parse(this.endTime, formatter);

        // Cria LocalDateTime combinando a data do evento com a hora atual
        LocalDateTime startDateTime = LocalDateTime.of(eventDate.toLocalDate(), startTime);
        LocalDateTime endDateTime = LocalDateTime.of(eventDate.toLocalDate(), endTime);

        // Verifica se o evento ocorre no mesmo dia do ano atual
        boolean sameDay = currentDateTime.getDayOfYear() == eventDate.getDayOfYear();

        // Verifica se a hora atual está entre startTime e endTime do evento
        boolean withinTimeRange = !currentDateTime.isBefore(startDateTime) &&
                !currentDateTime.isAfter(endDateTime);

        // Retorna true se estiver no mesmo dia e dentro do intervalo de tempo do evento
        return sameDay && withinTimeRange;
    }
}
