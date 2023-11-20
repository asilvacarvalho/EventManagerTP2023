package pt.isec.eventmanager.client;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import pt.isec.eventmanager.MainClient;
import pt.isec.eventmanager.events.Attendance;
import pt.isec.eventmanager.events.Event;
import pt.isec.eventmanager.util.LabelType;
import pt.isec.eventmanager.util.Utils;

import java.io.IOException;
import java.util.ArrayList;

public class ClientAuthenticatedController {
    @FXML
    private Button codeSubmitButton;
    @FXML
    private Button attendancesButton;
    @FXML
    private Button editProfileButton;
    @FXML
    private Button listEventsButton;
    @FXML
    private Button createEventButton;
    @FXML
    private Button checkAttendaceButton;
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

    private AnchorPane loadingPane;
    private PauseTransition infoPauseTransition;

    @FXML
    public void initialize() {
        this.loadingPane = loadingPane();
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
    public void handleAttendancesButtonAction() {
        initListUserEvents();
    }

    @FXML
    public void handleEditProfileButtonAction() {
        editUSer();
    }

    @FXML
    private void handleListEventButtonAction() {
        initListEvents();
    }

    @FXML
    private void handleCreateEventButtonAction() {
        createEvent();
    }

    @FXML
    public void handleCheckUserAttendaceButtonAction() {
        initAdminListUserEvents();
    }


    //PANE
    public void initClientAutheController(Stage stage, Client client) {
        this.mainStage = stage;
        this.client = client;
        initLayout();
    }

    public void initLayout() {
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

    public void showLoading() {
        mainContentArea.getChildren().clear();
        mainContentArea.getChildren().add(loadingPane);
    }


    //USER
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

    private void editUSer() {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/client-edit-user.fxml"));

            Pane editUserPane = loader.load();

            mainContentArea.getChildren().clear();
            mainContentArea.getChildren().add(editUserPane);

            Platform.runLater(() -> {
                EditUserController editUserController = loader.getController();
                editUserController.initEditUserController(client, this);
            });

        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading EditUserFXML");
        }
    }

    private void insertUserKey() {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/client-user-key.fxml"));

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

    public void initListUserEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/list-user-events.fxml"));

            Pane listUserEventsPane = loader.load();

            Thread thread = new Thread(() -> {
                ArrayList<Event> listEvents = client.listUserEvents("user");

                Platform.runLater(() -> {
                    mainContentArea.getChildren().clear();
                    mainContentArea.getChildren().add(listUserEventsPane);
                    ListUserEventsController listUserEventsController = loader.getController();
                    listUserEventsController.initListUserEventsController(listEvents, this, false, client);
                });
            });
            thread.start();

        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading ListEventaFXML");
        }
    }

    public void initAdminListUserEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/list-user-events.fxml"));

            Pane listUserEventsPane = loader.load();

            mainContentArea.getChildren().clear();
            mainContentArea.getChildren().add(listUserEventsPane);

            ArrayList<Event> listEvents = new ArrayList<>();

            Platform.runLater(() -> {
                ListUserEventsController listUserEventsController = loader.getController();
                listUserEventsController.initListUserEventsController(listEvents, this, true, client);
            });

        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading ListEventaFXML");
        }
    }


    //EVENTS
    public void initListEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/list-events.fxml"));

            Pane listEventsPane = loader.load();

            Thread thread = new Thread(() -> {
                ArrayList<Event> listEvents = client.listEvents();

                Platform.runLater(() -> {
                    mainContentArea.getChildren().clear();
                    mainContentArea.getChildren().add(listEventsPane);
                    ListEventsController listEventsController = loader.getController();
                    listEventsController.initListEventsController(listEvents, this);
                });
            });
            thread.start();

        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading ListEventaFXML");
        }
    }

    private void createEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/client-add-event.fxml"));

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
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/client-add-event.fxml"));

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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("Delete Event");
        alert.setContentText("Are you sure you want to delete this event?");

        ButtonType buttonTypeYes = new ButtonType("Yes");
        ButtonType buttonTypeNo = new ButtonType("No");

        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == buttonTypeYes) {
                showLoading();

                Thread thread = new Thread(() -> {
                    boolean success = client.deleteEvent(event);

                    Platform.runLater(() -> {
                        if (success) {
                            showInfo("Event deleted successfully", LabelType.INFO);
                        } else {
                            showInfo("Error deleting event", LabelType.ERROR);
                        }
                        initListEvents();
                    });
                });

                thread.start();
            }
        });
    }

    public void showEventAttendances(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/list-attendances.fxml"));

            Pane listAttendancesPane = loader.load();

            Thread thread = new Thread(() -> {
                ArrayList<Attendance> listAttendances = client.listAttendences(event.getId());

                Platform.runLater(() -> {
                    mainContentArea.getChildren().clear();
                    mainContentArea.getChildren().add(listAttendancesPane);
                    ListAttendancesController listAttendancesController = loader.getController();
                    listAttendancesController.initListAttendancesController(listAttendances, this, event);
                });
            });
            thread.start();

        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading ListEventaFXML");
        }
    }

    public void generateEventKey(Event event) {
        if (event.isEventInProgress()) {
            try {
                FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/client-event-key.fxml"));

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

    public void addEventAttendance(int eventId, String username) {
        Attendance attendance = new Attendance(eventId, username);

        Thread thread = new Thread(() -> {
            boolean success = client.addAttendance(attendance);

            Platform.runLater(() -> {
                if (success) {
                    showInfo("Operation successfully", LabelType.INFO);
                } else {
                    showInfo("Operation Error!", LabelType.ERROR);
                }
            });
        });
        thread.start();
    }

    public void deleteEventAttendance(Attendance registration, Event event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Delete Attendance");
        alert.setContentText("Are you sure you want to delete this attendance?");

        ButtonType buttonTypeYes = new ButtonType("Yes");
        ButtonType buttonTypeNo = new ButtonType("No");

        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == buttonTypeYes) {
                showLoading();

                Thread thread = new Thread(() -> {
                    boolean success = client.deleteAttendance(registration);

                    Platform.runLater(() -> {
                        if (success) {
                            showInfo("Operation successfully", LabelType.INFO);
                        } else {
                            showInfo("Operation Error!", LabelType.ERROR);
                        }
                        showEventAttendances(event);
                    });
                });

                thread.start();
            }
        });
    }

    private AnchorPane loadingPane() {
        AnchorPane pane = new AnchorPane();
        Label loadingLabel = new Label("Loading...");

        AnchorPane.setLeftAnchor(loadingLabel, 0.0);
        AnchorPane.setRightAnchor(loadingLabel, 0.0);
        AnchorPane.setTopAnchor(loadingLabel, 0.0);
        AnchorPane.setBottomAnchor(loadingLabel, 0.0);

        pane.getChildren().add(loadingLabel);

        return pane;
    }
}
