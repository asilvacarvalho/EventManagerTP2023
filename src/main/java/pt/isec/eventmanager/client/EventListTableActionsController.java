package pt.isec.eventmanager.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import pt.isec.eventmanager.events.Event;

public class EventListTableActionsController {
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
        generateKeyButton.setTooltip(new Tooltip("Gerar Chave"));
        infoButton.setTooltip(new Tooltip("Informações"));
        editButton.setTooltip(new Tooltip("Editar"));
        deleteButton.setTooltip(new Tooltip("Apagar"));
    }

    public void initListActionController(ClientAuthenticatedController controller) {
        this.clientAuthenticatedController = controller;
    }

    public void initEvent(Event event) {
        this.listItem = event;
    }

    @FXML
    void handleDeleteAction(ActionEvent event) {
        if (clientAuthenticatedController != null)
            clientAuthenticatedController.deleteEvent(listItem);
    }

    @FXML
    void handleEditAction(ActionEvent event) {
        if (clientAuthenticatedController != null)
            clientAuthenticatedController.editEvent(listItem);
    }

    @FXML
    void handleGenerateKeyAction(ActionEvent event) {
        if (clientAuthenticatedController != null)
            clientAuthenticatedController.generateEventKey(listItem);
    }

    @FXML
    void handleInfoAction(ActionEvent event) {
        if (clientAuthenticatedController != null)
            clientAuthenticatedController.showEventAttendances(listItem);
    }
}
