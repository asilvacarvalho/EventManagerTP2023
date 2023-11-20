package pt.isec.eventmanager.client;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import pt.isec.eventmanager.MainClient;
import pt.isec.eventmanager.util.LabelType;
import pt.isec.eventmanager.util.Utils;

import java.io.IOException;

public class ClientLoginController {
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField studentNumberField;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;
    @FXML
    private Label infoLabel;

    private Stage mainStage;
    private Client client;

    private boolean registering = false;

    @FXML
    private void initialize() {
        //TODO: remove after testing
        emailField.setText("admin");
        passwordField.setText("admin");
    }

    @FXML
    private void handleLoginButtonAction() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty() || client.getServerAddress().isEmpty() || client.getServerPort().isEmpty()) {
            System.out.println("[ClientLoginController] Information missing to start the client!");
            showInfo("Information missing to start the client!", LabelType.ERROR);
            return;
        }

        Thread thread = new Thread(() -> {
            boolean success = client.login(email, password);

            Platform.runLater(() -> {
                if (success) {
                    showAuthenticatedUI();
                } else {
                    System.out.println("[ClientLoginController] Login Error!");
                    showInfo("Login Error!", LabelType.ERROR);
                }
            });
        });

        thread.start();
    }

    @FXML
    private void handleRegisterButtonAction() {
        if (registering) {
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();
            String name = nameField.getText().trim();
            String studentNumber = studentNumberField.getText().trim();

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || studentNumber.isEmpty()) {
                showInfo("Missing elements to register new user!", LabelType.ERROR);
                clearTextFields();
                hideRegistrationNewUser();
            } else {
                Thread thread = new Thread(() -> {
                    boolean success = client.registerUser(email, password, name, studentNumber);

                    Platform.runLater(() -> {
                        if (success) {
                            System.out.println("[ClientLoginController] New User Registration Success!");
                            showInfo("New User Registration Success!", LabelType.INFO);
                            hideRegistrationNewUser();
                        } else {
                            System.err.println("[ClientLoginController] New User Registration Error!");
                            showInfo("Registration Error!", LabelType.ERROR);
                            clearTextFields();
                            hideRegistrationNewUser();
                        }
                    });
                });
                thread.start();
            }
        } else {
            showRegistrationNewUser();
        }
    }


    public void initLoginController(Stage stage, Client client) {
        this.mainStage = stage;
        this.client = client;
    }

    public void handleLogout(Stage stage, Client client) {
        this.mainStage = stage;
        this.client = client;
        this.client.logout();
        clearTextFields();
    }

    private void showAuthenticatedUI() {
        System.out.println("[LoginController] Login Successful from " + client.getUser().getName());
        try {
            FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/client-authenticated.fxml"));
            Scene scene = new Scene(loader.load());

            mainStage.setScene(scene);
            mainStage.show();

            Platform.runLater(() -> {
                ClientAuthenticatedController authenticatedController = loader.getController();
                authenticatedController.initClientAutheController(mainStage, client);
            });
        } catch (IOException e) {
            System.out.println("[ClienteController] Error loading ClientAuthenticatedController");
        }
    }

    private void clearTextFields() {
        nameField.setText("");
        studentNumberField.setText("");
        emailField.setText("");
        passwordField.setText("");
    }

    private void showRegistrationNewUser() {
        clearTextFields();
        nameField.setVisible(true);
        studentNumberField.setVisible(true);
        loginButton.setVisible(false);
        registerButton.setText("Register");
        registering = true;
    }

    private void hideRegistrationNewUser() {
        nameField.setVisible(false);
        studentNumberField.setVisible(false);
        loginButton.setVisible(true);
        registerButton.setText("New User");
        registering = false;
    }

    private void showInfo(String msg, LabelType type) {
        infoLabel.setStyle(Utils.getLabelStyle(type));
        infoLabel.setText(msg);
        infoLabel.setVisible(true);

        PauseTransition visiblePause = new PauseTransition(Duration.seconds(7));
        visiblePause.setOnFinished(event -> infoLabel.setVisible(false));
        visiblePause.play();
    }
}
