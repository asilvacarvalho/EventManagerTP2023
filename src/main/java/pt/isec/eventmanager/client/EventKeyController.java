package pt.isec.eventmanager.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.events.EventKey;
import pt.isec.eventmanager.util.LabelType;

import java.util.Calendar;
import java.util.Date;

public class EventKeyController {
    @FXML
    private TextField currentCodeField;
    @FXML
    private TextField currentDurationField;
    @FXML
    private VBox currentKeyBox;
    @FXML
    private VBox generateKeyBox;
    @FXML
    private TextField durationField;
    @FXML
    private VBox newEventKeyBox;
    @FXML
    private TextField newEventKeyTextField;
    @FXML
    private VBox eventKeyMainBox;

    private Client client;
    private ClientAuthenticatedController parentController;
    private Event event;

    @FXML
    public void initialize() {
    }

    @FXML
    public void handleSaveEvent() {
        if (durationField.getText().isEmpty()) return;

        int durationMinutes;
        try {
            durationMinutes = Integer.parseInt(durationField.getText());
            if (durationMinutes <= 0) {
                parentController.showInfo("Invalid duration value!", LabelType.ERROR);
                return;
            }
        } catch (NumberFormatException e) {
            parentController.showInfo("Invalid duration format!", LabelType.ERROR);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, durationMinutes);
        Date newDate = calendar.getTime();

        EventKey eventKey = new EventKey(event.getId());
        eventKey.setEndDate(newDate);

        eventKey.generateCode();

        Thread thread = new Thread(() -> {
            boolean success = client.insertEventKey(eventKey);

            Platform.runLater(() -> {
                if (success) {
                    parentController.showInfo("Operation successfully", LabelType.INFO);
                    eventKeyMainBox.getChildren().remove(currentKeyBox);
                    eventKeyMainBox.getChildren().remove(generateKeyBox);
                    newEventKeyTextField.setText(eventKey.getCode().toString());
                    newEventKeyBox.setVisible(true);
                } else {
                    parentController.showInfo("Operation Error!", LabelType.ERROR);
                }
            });
        });
        thread.start();
    }

    public void initEventKeyController(Client client, ClientAuthenticatedController controller, Event event) {
        this.parentController = controller;
        this.client = client;
        this.event = event;

        Thread thread = new Thread(() -> {
            EventKey currentKey = client.getEventKey(event);

            Platform.runLater(() -> {
                if (currentKey != null) {
                    currentCodeField.setText(currentKey.getCode().toString());
                    currentDurationField.setText(currentKey.getEndDate().toString());
                } else {
                    currentKeyBox.setVisible(false);
                }
            });
        });
        thread.start();
    }
}
