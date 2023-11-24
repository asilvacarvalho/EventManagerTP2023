package pt.isec.eventmanager.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import pt.isec.eventmanager.events.Event;

public class ListEventsActionsController {
    @FXML
    private Button generateKeyButton;

    @FXML
    private Button infoButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    private ClientAuthenticatedController clientAuthenticatedController;
    private Event listItem;

    @FXML
    public void initialize() {
        generateKeyButton.setTooltip(new Tooltip("Generate Key"));
        infoButton.setTooltip(new Tooltip("Attendances"));
        editButton.setTooltip(new Tooltip("Edit"));
        deleteButton.setTooltip(new Tooltip("Delete"));
    }

    public void initListActionController(ClientAuthenticatedController controller) {
        this.clientAuthenticatedController = controller;
    }

    public void initEvent(Event event) {
        this.listItem = event;
    }

    @FXML
    void handleDeleteAction() {
        if (clientAuthenticatedController != null && listItem != null)
            clientAuthenticatedController.deleteEvent(listItem);
    }

    @FXML
    void handleEditAction() {
        if (clientAuthenticatedController != null && listItem != null)
            clientAuthenticatedController.editEvent(listItem);
    }

    @FXML
    void handleGenerateKeyAction() {
        if (clientAuthenticatedController != null && listItem != null)
            clientAuthenticatedController.generateEventKey(listItem);
    }

    @FXML
    void handleInfoAction() {
        if (clientAuthenticatedController != null && listItem != null)
            clientAuthenticatedController.listEventAttendances(listItem);
    }
}
