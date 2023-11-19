package pt.isec.eventmanager.client;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import pt.isec.eventmanager.MainClient;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.util.LabelType;
import pt.isec.eventmanager.util.Utils;

import java.io.IOException;
import java.util.ArrayList;

public class ClientAuthenticatedController {
    @FXML
    private Button attendancesButton;
    @FXML
    private Button checkAttendaceButton;
    @FXML
    private Button codeSubmitButton;
    @FXML
    private Button createEventButton;
    @FXML
    private Button editProfileButton;
    @FXML
    private Button listEventsButton;
    @FXML
    private VBox menuOptionsPane;
    @FXML
    private VBox mainContentArea;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label infoLabel;

    private Stage mainStage;
    private Client client;

    private PauseTransition infoPauseTransition;

    @FXML
    public void initialize() {
    }

    public void initClientAutheController(Stage stage, Client client) {
        this.mainStage = stage;
        this.client = client;
        initLayout();
    }

    private void initLayout() {
        welcomeLabel.setText("Welcome " + client.getUser().getName());
        if (client.getUser().isAdmin()) initAdmin();
    }

    private void initAdmin() {
        menuOptionsPane.getChildren().remove(codeSubmitButton);
        menuOptionsPane.getChildren().remove(attendancesButton);
        menuOptionsPane.getChildren().remove(editProfileButton);
        createEventButton.setVisible(true);
        listEventsButton.setVisible(true);
        checkAttendaceButton.setVisible(true);
    }

    @FXML
    private void handleLogoutButtonAction() {
        logout();
    }

    @FXML
    public void handleCodeSubmitButtonAction() {
        insertUserKey();
    }

    @FXML
    private void handlecreateEventButtonAction() {
        createEvent();
    }

    @FXML
    private void handlelistEventButtonAction() {
        initTableViewContent();
    }

    public void clearMainContentArea() {
        mainContentArea.getChildren().clear();
    }

    public void showInfo(String msg, LabelType type) {
        if (infoPauseTransition != null)
            infoPauseTransition.stop();

        infoLabel.setStyle(Utils.getLabelStyle(type));
        infoLabel.setText(msg);
        infoLabel.setVisible(true);

        infoPauseTransition = new PauseTransition(Duration.seconds(8));
        infoPauseTransition.setOnFinished(event -> infoLabel.setVisible(false));
        infoPauseTransition.play();
    }

    private void logout() {
        System.out.println("[ClientAuthenticatedController] Loggin out");
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/client-login.fxml"));
            Scene scene = new Scene(loader.load());

            mainStage.setScene(scene);
            mainStage.show();

            Platform.runLater(() -> {
                ClientLoginController clientLoginController = loader.getController();
                clientLoginController.handleLogout(mainStage, client);
            });
        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading ClientAuthenticatedFXML");
        }
    }

    public void initTableViewContent() {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/list-events.fxml"));

            Pane listEventsPane = loader.load();

            mainContentArea.getChildren().clear();
            mainContentArea.getChildren().add(listEventsPane);

            ArrayList<Event> listEvents = client.listEvents();

            Platform.runLater(() -> {
                ListEventsController listEventsController = loader.getController();
                listEventsController.initListEventsController(listEvents, this);
            });

        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading ListEventaFXML");
        }
    }

    private void createEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/add-event.fxml"));

            Pane addEventPane = loader.load();

            mainContentArea.getChildren().clear();
            mainContentArea.getChildren().add(addEventPane);

            Platform.runLater(() -> {
                AddEventController addEventController = loader.getController();
                addEventController.initAddEventController(client, this);
            });

        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading AddEventFXML");
        }
    }

    public void editEvent(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/add-event.fxml"));

            Pane addEventPane = loader.load();

            mainContentArea.getChildren().clear();
            mainContentArea.getChildren().add(addEventPane);

            Platform.runLater(() -> {
                AddEventController addEventController = loader.getController();
                addEventController.initEditEventController(client, this, event);
            });

        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading AddEventFXML");
        }
    }

    public void deleteEvent(Event event) {
        System.out.println("[ClientAuthenticationController] Delete Event " + event.getId());
    }

    public void showEventAttendances(Event event) {
        System.out.println("[ClientAuthenticationController] Show Attendances from Event " + event.getId());
    }

    public void generateEventKey(Event event) {
        if (event.isEventInProgress()) {
            try {
                FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/event-key.fxml"));

                Pane generateKeyPane = loader.load();

                mainContentArea.getChildren().clear();
                mainContentArea.getChildren().add(generateKeyPane);

                Platform.runLater(() -> {
                    EventKeyController eventKeyController = loader.getController();
                    eventKeyController.initEventKeyController(client, this, event);
                });

            } catch (IOException e) {
                System.out.println("[ClienteController] Error loading AddEventFXML");
            }
        } else {
            showInfo("Only active events can generate keys!", LabelType.ERROR);
        }
    }

    private void insertUserKey() {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/user-key.fxml"));

            Pane insertUserKeyPane = loader.load();

            mainContentArea.getChildren().clear();
            mainContentArea.getChildren().add(insertUserKeyPane);

            Platform.runLater(() -> {
                UserKeyController userKeyController = loader.getController();
                userKeyController.initUserKeyController(client, this);
            });

        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading AddEventFXML");
        }
    }
}
