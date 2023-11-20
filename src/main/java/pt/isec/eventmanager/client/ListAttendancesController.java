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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private ClientAuthenticatedController parentController;
    private Event event;

    @FXML
    public void initialize() {
        initOptionsColumn();
    }

    @FXML
    public void addAttendanceButtonAction() {
        addAttendanceVBox.setVisible(!addAttendanceVBox.isVisible());
    }

    @FXML
    void saveAttendanceButtonAction() {
        if (event == null || parentController == null) return;

        String username = usernameField.getText();

        if (username.isEmpty()) {
            parentController.showInfo("Username can't be empty!", LabelType.ERROR);
            return;
        }

        parentController.addEventAttendance(event.getId(), username);
        addAttendanceVBox.setVisible(false);
    }

    @FXML
    public void generateCSVButtonAction() {
        List<Attendance> attendanceList = attendancesTableView.getItems();

        if (attendanceList.isEmpty())
            return;

        // Obtenha a data e hora atual para incluir no nome do arquivo CSV
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss");
        String formattedDateTime = now.format(formatter);
        File csvFile = new File(formattedDateTime + "_event_" + event.getId() + "_" + event.getName() + "_attendances.csv");

        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("EventID: " + event.getId() + " EventName: " + event.getName());
            writer.write(" EventLocation: " + event.getLocation() + " EventDate: " + event.getDate());
            writer.write(" EventStartTime: " + event.getId() + " EventEndTime: " + event.getEndTime() + "\n");
            writer.write("Username\n");

            for (Attendance attendance : attendanceList) {
                String line = String.format("%s\n", attendance.getUsername());
                writer.write(line);
            }

            System.out.println("CSV File Saved at " + csvFile.getAbsolutePath());
            parentController.showInfo("CSV File Saved", LabelType.INFO);
        } catch (IOException e) {
            System.err.println("Error creating CSV file: " + e.getMessage());
            parentController.showInfo("Error creating CSV", LabelType.ERROR);
        }
    }

    public void initListAttendancesController(ArrayList<Attendance> listAttendances, ClientAuthenticatedController controller, Event event) {
        this.parentController = controller;
        this.event = event;
        attendancesTableView.getItems().addAll(listAttendances);
        initEventInfo();
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
                                listAttendancesAtionsController.initListAttendancesController(parentController, event);
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
}
