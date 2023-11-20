package pt.isec.eventmanager.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.util.LabelType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ListUserEventsController {
    @FXML
    public HBox userHBox;
    @FXML
    public TextField userField;
    @FXML
    private TableColumn<Event, Date> dateColumn;
    @FXML
    private TableView<Event> eventTableView;

    private Client client;
    private ClientAuthenticatedController parentController;

    @FXML
    public void initialize() {
        userHBox.setVisible(false);
        formateDateColumn();
    }

    @FXML
    public void handleSearchEvent() {
        if (parentController == null || userField.getText().isEmpty() || client == null)
            return;

        ArrayList<Event> listEvents = client.listUserEvents(userField.getText());
        eventTableView.getItems().clear();
        eventTableView.getItems().addAll(listEvents);
    }

    @FXML
    void generateCSVButtonAction() {
        List<Event> eventList = eventTableView.getItems();

        if (eventList.isEmpty())
            return;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss");
        String formattedDateTime = now.format(formatter);

        String filename;

        if (client.getUser().isAdmin())
            filename = formattedDateTime + "_user_" + userField.getText() + "_attendances.csv";
        else
            filename = formattedDateTime + "_user_" + client.getUser().getName() + "_attendances.csv";

        Thread thread = new Thread(() -> {
            File csvFile = new File(filename);

            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.write("Name,Date,Location,Start Time,End Time\n");

                SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
                for (Event event : eventList) {
                    String formattedDate = dateFormatter.format(event.getDate());

                    String line = String.format("%s,%s,%s,%s,%s\n", event.getName(), event.getLocation(), formattedDate, event.getStartTime(), event.getEndTime());
                    writer.write(line);
                }

                System.out.println("CSV File Saved at " + csvFile.getAbsolutePath());
                Platform.runLater(() -> {
                    parentController.showInfo("CSV File Saved", LabelType.INFO);
                });

            } catch (IOException e) {
                System.err.println("Error creating CSV file: " + e.getMessage());
                Platform.runLater(() -> {
                    parentController.showInfo("Error creating CSV", LabelType.ERROR);
                });
            }
        });
        thread.start();
    }

    public void initListUserEventsController(ArrayList<Event> listEvents, ClientAuthenticatedController controller, boolean admin, Client client) {
        this.client = client;
        this.parentController = controller;
        eventTableView.getItems().addAll(listEvents);

        if (admin)
            userHBox.setVisible(true);
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
}
