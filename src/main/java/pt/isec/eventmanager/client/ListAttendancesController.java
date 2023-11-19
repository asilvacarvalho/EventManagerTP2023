package pt.isec.eventmanager.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import pt.isec.eventmanager.MainClient;
import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.util.LabelType;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

public class ListAttendancesController {
    @FXML
    private VBox addAttendanceVBox;
    @FXML
    public DatePicker datePicker;
    @FXML
    private TextField endTimeField;
    @FXML
    private TableView<Attendance> attendancesTableView;
    @FXML
    private TextField locationField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField startTimeField;
    @FXML
    private TextField usernameField;
    @FXML
    private TableColumn<Attendance, String> optionsColumn;

    private ClientAuthenticatedController clientAuthenticatedController;
    private Event event;

    @FXML
    public void initialize() {
        initOptionsColumn();
    }

    private void initOptionsColumn() {
        Callback<TableColumn<Attendance, String>, TableCell<Attendance, String>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Attendance, String> call(final TableColumn<Attendance, String> param) {
                return new TableCell<>() {
                    private ListAttendancesActionsController listAttendancesAtionsController;

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        try {
                            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/list-attendances-actions.fxml"));
                            AnchorPane listActions = loader.load();

                            Platform.runLater(() -> {
                                listAttendancesAtionsController = loader.getController();
                                listAttendancesAtionsController.initListAttendancesController(clientAuthenticatedController);
                            });

                            if (empty) {
                                setGraphic(null);
                                setText(null);
                            } else {
                                Attendance registrationAux = getTableView().getItems().get(getIndex());
                                Platform.runLater(() -> listAttendancesAtionsController.initAttendance(registrationAux));
                                setGraphic(listActions);
                                setText(null);
                            }

                        } catch (IOException e) {
                            System.out.println("[ClienteController] Error loading ListEventaFXML");
                        }
                    }
                };
            }
        };

        optionsColumn.setCellFactory(cellFactory);
    }

    public void initListAttendancesController(ArrayList<Attendance> listAttendances, ClientAuthenticatedController controller, Event event) {
        this.clientAuthenticatedController = controller;
        this.event = event;
        attendancesTableView.getItems().addAll(listAttendances);
        initEventInfo();
    }

    private void initEventInfo() {
        if (event != null) {
            nameField.setText(event.getName());
            locationField.setText(event.getLocation());
            startTimeField.setText(event.getStartTime());
            endTimeField.setText(event.getEndTime());

            Date date = event.getDate();
            Instant instant = date.toInstant();
            LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            datePicker.setValue(localDate);
        }
    }

    @FXML
    public void addAttendanceButtonAction() {
        addAttendanceVBox.setVisible(!addAttendanceVBox.isVisible());
    }

    @FXML
    void saveAttendanceButtonAction() {
        if (event == null || clientAuthenticatedController == null) return;

        String username = usernameField.getText();

        if (username.isEmpty()) {
            clientAuthenticatedController.showInfo("Username can't be empty!", LabelType.ERROR);
            return;
        }

        clientAuthenticatedController.addEventAttendance(event.getId(), username);
        addAttendanceVBox.setVisible(false);
    }

    @FXML
    public void generateCSVButtonAction() {
    }
}
