package pt.isec.eventmanager.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import pt.isec.eventmanager.MainClient;
import pt.isec.eventmanager.events.Event;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ListEventsController {
    @FXML
    public TextField searchField;
    @FXML
    private TableColumn<Event, Date> dateColumn;
    @FXML
    private TableView<Event> eventTableView;
    @FXML
    private TableColumn<Event, String> optionsColumn;

    private ClientAuthenticatedController clientAuthenticatedController;
    private ArrayList<Event> listEvents;

    @FXML
    public void initialize() {
        formateDateColumn();
        initOptionsColumn();
    }

    @FXML
    public void handleSearchButtonAction() {
        String searchText = searchField.getText().trim();

        if (!searchText.isEmpty()) {
            ArrayList<Event> filteredEvents = new ArrayList<>();

            for (Event event : eventTableView.getItems()) {
                if (event.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                        event.getLocation().toLowerCase().contains(searchText.toLowerCase()) ||
                        event.getDate().toString().contains(searchText)) {
                    filteredEvents.add(event);
                }
            }

            eventTableView.getItems().clear();
            eventTableView.getItems().addAll(filteredEvents);
        } else {
            eventTableView.getItems().clear();
            eventTableView.getItems().addAll(listEvents);
        }
    }

    @FXML
    public void handleClearButtonAction() {
        searchField.setText("");
        eventTableView.getItems().clear();
        eventTableView.getItems().addAll(listEvents);
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

    private void initOptionsColumn() {
        Callback<TableColumn<Event, String>, TableCell<Event, String>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Event, String> call(final TableColumn<Event, String> param) {
                return new TableCell<>() {
                    private ListEventsActionsController listEventsController;

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        try {
                            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/list-events-actions.fxml"));
                            AnchorPane listActions = loader.load();

                            Platform.runLater(() -> {
                                listEventsController = loader.getController();
                                listEventsController.initListActionController(clientAuthenticatedController);
                            });

                            if (empty) {
                                setGraphic(null);
                                setText(null);
                            } else {
                                Event eventAux = getTableView().getItems().get(getIndex());
                                Platform.runLater(() -> listEventsController.initEvent(eventAux));
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

    public void initListEventsController(ArrayList<Event> listEvents, ClientAuthenticatedController controller) {
        this.clientAuthenticatedController = controller;
        this.listEvents = listEvents;
        eventTableView.getItems().clear();
        eventTableView.getItems().addAll(listEvents);
    }
}
