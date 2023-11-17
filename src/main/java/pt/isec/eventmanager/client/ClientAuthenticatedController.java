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
    private Button generateCodeButton;
    @FXML
    private Button listEventsButton;
    @FXML
    private Button logoutButton;
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
        generateCodeButton.setVisible(true);
        checkAttendaceButton.setVisible(true);
    }

    @FXML
    private void handleLogoutButtonAction() {
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

    @FXML
    private void handlecreateEventButtonAction() {
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

    @FXML
    private void handlelistEventButtonAction() {
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

    public void showInfo(String msg, LabelType type) {
        infoLabel.setStyle(Utils.getLabelStyle(type));
        infoLabel.setText(msg);
        infoLabel.setVisible(true);

        PauseTransition visiblePause = new PauseTransition(Duration.seconds(3));
        visiblePause.setOnFinished(event -> infoLabel.setVisible(false));
        visiblePause.play();
    }

    public void editEvent(Event event) {
        System.out.println("[ClientAuthenticationController] Edit Event " + event.getId());
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
        System.out.println("[ClientAuthenticationController] Generate Key for Event " + event.getId());
    }
}
