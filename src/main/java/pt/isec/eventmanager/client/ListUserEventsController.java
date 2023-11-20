package pt.isec.eventmanager.client;

import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import pt.isec.eventmanager.events.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ListUserEventsController {
    @FXML
    public HBox userHBox;
    @FXML
    public TextField userField;
    @FXML
    private TableColumn<Event, Date> dateColumn;
    @FXML
    private TableView<Event> eventTableView;

    private ClientAuthenticatedController parentController;

    @FXML
    public void initialize() {
        userHBox.setVisible(false);
        formateDateColumn();
    }

    private void formateDateColumn() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");

        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateFormatter.format(item));
                }
            }
        });
    }

    public void initListUserEventsController(ArrayList<Event> listEvents, ClientAuthenticatedController controller, boolean admin) {
        this.parentController = controller;
        eventTableView.getItems().addAll(listEvents);

        if (admin)
            userHBox.setVisible(true);
    }

    @FXML
    void generateCSVButtonAction() {

    }

    @FXML
    public void handleSearchEvent() {
        if (parentController == null && userField.getText().isEmpty())
            return;

        ArrayList<Event> listEvents = parentController.getClient().listUserEvents(userField.getText());
        eventTableView.getItems().clear();
        eventTableView.getItems().addAll(listEvents);
    }
}
