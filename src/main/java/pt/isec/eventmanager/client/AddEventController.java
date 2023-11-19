package pt.isec.eventmanager.client;

import javafx.fxml.FXML;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.util.LabelType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

public class AddEventController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField locationField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField startTimeField;
    @FXML
    private TextField endTimeField;

    private ClientAuthenticatedController parentController;
    private Client client;

    private Event eventEdit;

    @FXML
    public void initialize() {
        datePicker.setDayCellFactory(picker -> new DateCell() {
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate today = LocalDate.now();

                setDisable(empty || date.isBefore(today));
            }
        });
    }

    public void initAddEventController(Client client, ClientAuthenticatedController controller) {
        this.client = client;
        this.parentController = controller;
    }

    public void initEditEventController(Client client, ClientAuthenticatedController controller, Event event) {
        this.eventEdit = event;
        this.client = client;
        this.parentController = controller;

        nameField.setText(eventEdit.getName());
        locationField.setText(eventEdit.getLocation());
        startTimeField.setText(eventEdit.getStartTime());
        endTimeField.setText(eventEdit.getEndTime());

        Date date = eventEdit.getDate();
        Instant instant = date.toInstant();
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        datePicker.setValue(localDate);

        if (client.checkEventHasAttendences(eventEdit.getId())) {
            startTimeField.setDisable(true);
            endTimeField.setDisable(true);
            datePicker.setDisable(true);
        }
    }

    @FXML
    private void handleSaveEvent() {
        if (nameField.getText().isEmpty()
                || locationField.getText().isEmpty()
                || datePicker.getValue() == null
                || startTimeField.getText().isEmpty()
                || endTimeField.getText().isEmpty()) {
            parentController.showInfo("Missing elements to create event", LabelType.ERROR);
            return;
        }

        if (!startTimeField.getText().matches("(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]")) {
            startTimeField.setText("");
            parentController.showInfo("Time must be in the format HH:mm!", LabelType.ERROR);
            return;
        }

        if (!endTimeField.getText().matches("(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]")) {
            endTimeField.setText("");
            parentController.showInfo("Time must be in the format HH:mm!", LabelType.ERROR);
            return;
        }

        LocalTime start = LocalTime.parse(startTimeField.getText());
        LocalTime end = LocalTime.parse(endTimeField.getText());
        if (end.isBefore(start)) {
            parentController.showInfo("End time must be after start time!", LabelType.ERROR);
            return;
        }

        String eventName = nameField.getText();
        String eventLocation = locationField.getText();
        Date eventDate = Date.from(Instant.from(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        String eventStartTime = startTimeField.getText();
        String eventEndTime = endTimeField.getText();

        boolean success;

        if (eventEdit != null) {
            eventEdit.setName(eventName);
            eventEdit.setLocation(eventLocation);
            eventEdit.setDate(eventDate);
            eventEdit.setStartTime(eventStartTime);
            eventEdit.setEndTime(eventEndTime);
            success = client.editEvent(eventEdit);
        } else {
            success = client.addEvent(new Event(eventName, eventLocation, eventDate, eventStartTime, eventEndTime));
        }

        if (success) {
            parentController.showInfo("Operation successfully", LabelType.INFO);
        } else {
            parentController.showInfo("Operation Error!", LabelType.ERROR);
        }

        clearForm();
    }

    private void clearForm() {
        nameField.setText("");
        locationField.setText("");
        datePicker.setValue(null);
        startTimeField.setText("");
        endTimeField.setText("");
        eventEdit = null;
    }
}
