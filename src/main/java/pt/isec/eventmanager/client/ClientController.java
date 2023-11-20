package pt.isec.eventmanager.client;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import pt.isec.eventmanager.MainClient;

import java.io.IOException;

public class ClientController {
    @FXML
    private TextField serverAddressField;
    @FXML
    private TextField serverPortField;
    @FXML
    private Label errorLabel;

    private Stage mainStage;
    private Client client;


    @FXML
    private void initialize() {
        //TODO: remove after testing
        serverAddressField.setText("localhost");
        serverPortField.setText("6000");
    }

    public void initClientController(Stage stage, Client client) {
        this.mainStage = stage;
        this.client = client;
    }

    @FXML
    private void handleConnectServerButtonAction() {
        String serverAddress = serverAddressField.getText().trim();
        String serverPort = serverPortField.getText().trim();

        if (serverAddress.isEmpty() || serverPort.isEmpty()) {
            System.out.println("[ClientController] Information missing to start the client!");
            showError("Information missing to start the client!");
        } else {
            Thread thread = new Thread(() -> {
                String connectionStatus = client.connect(serverAddress, serverPort);

                if (connectionStatus == null) {
                    System.out.println("[ClientController] Connection established");
                    try {
                        FXMLLoader loader = new FXMLLoader(MainClient.class.getResource("fxml/client-login.fxml"));
                        Scene scene = new Scene(loader.load());

                        Platform.runLater(() -> {
                            mainStage.setScene(scene);
                            mainStage.show();
                            ClientLoginController loginController = loader.getController();
                            loginController.initLoginController(mainStage, client);
                        });
                    } catch (IOException e) {
                        System.out.println("[ClienteController] Error loading ClientAuthenticatedController");
                        Platform.runLater(() -> {
                            showError("Error loading ClientAuthenticatedController");
                        });
                    }
                } else {
                    System.out.println("[ClienteController] " + connectionStatus);
                    Platform.runLater(() -> {
                        showError("Error creating connection socket!");
                    });
                }
            });

            thread.start();
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);

        PauseTransition visiblePause = new PauseTransition(Duration.seconds(3));
        visiblePause.setOnFinished(event -> errorLabel.setVisible(false));
        visiblePause.play();
    }
}
