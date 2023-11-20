package pt.isec.eventmanager.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;

public class ListAttendancesActionsController {
    @FXML
    private Button deleteButton;

    private ClientAuthenticatedController clientAuthenticatedController;
    private Event event;
    private Attendance listItem;

    @FXML
    public void initialize() {
        deleteButton.setTooltip(new Tooltip("Delete"));
    }

    public void initListAttendancesController(ClientAuthenticatedController controller, Event event) {
        this.clientAuthenticatedController = controller;
        this.event = event;
    }

    public void initAttendance(Attendance registration) {
        this.listItem = registration;
    }

    @FXML
    void handleDeleteAction() {
        if (clientAuthenticatedController != null && listItem != null && event != null)
            clientAuthenticatedController.deleteEventAttendance(listItem, event);
    }
}
