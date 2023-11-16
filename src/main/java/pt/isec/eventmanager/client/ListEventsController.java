package pt.isec.eventmanager.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private TableColumn<Event, Date> dateColumn;
    @FXML
    private TableView<Event> eventTableView;
    @FXML
    private TableColumn<Event, String> optionsColumn;

    private ClientAuthenticatedController clientAuthenticatedController;

    @FXML
    public void initialize() {
        formateDateColumn();
        initOptionsColumn();
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
                    private  EventListTableActionsController listEventsController;

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        try {
                            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/event-list-table-actions.fxml"));
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
                                Platform.runLater(() -> {
                                    listEventsController.initEvent(eventAux);
                                });
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
        eventTableView.getItems().addAll(listEvents);
    }

    @FXML
    private void deleteEvent(ActionEvent event) {
        System.out.println(event);
    }

    private Button createButton(String imageUrl, String action) {
        Button button = new Button();
        button.setGraphic(createImageView(imageUrl));
        button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        button.setOnAction(event -> {
            // Define a ação do botão aqui com base no parâmetro 'action'
            // Exemplo: if (action.equals("#handleGenerateKeyAction")) { ... }
        });
        button.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        button.setPrefSize(40, 40); // Defina o tamanho preferencial dos botões aqui
        button.setMaxSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        return button;
    }

    private ImageView createImageView(String imageUrl) {
        ImageView imageView = new ImageView(new Image(imageUrl));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(24); // Defina o tamanho da imagem aqui
        imageView.setFitHeight(24); // Defina o tamanho da imagem aqui
        imageView.setPickOnBounds(true);
        return imageView;
    }
}
