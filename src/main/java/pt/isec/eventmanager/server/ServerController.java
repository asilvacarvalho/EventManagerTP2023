package pt.isec.eventmanager.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerController {
    @FXML
    private TextField clientTcpPortField;
    @FXML
    private TextField regPortField;
    @FXML
    private TextField dbLocationField;
    @FXML
    private TextField rmiBackupServiceNameField;
    @FXML
    private TextArea consoleTextArea;
    @FXML
    private Button startButton;
    @FXML
    private Label errorLabel;

    private Server server;
    private Thread serverThread;
    private boolean started = false;


    @FXML
    private void initialize() {
        //TODO: Remove after testing
        clientTcpPortField.setText("6000");
        regPortField.setText("5000");
        dbLocationField.setText("./DB/eventmanagerdb.sqlite");
        rmiBackupServiceNameField.setText("eventmanagerbackup");
    }

    @FXML
    private void handleStartButtonAction() {
        hideError();

        if (!started) {
            addToConsole("Starting server");
            startServer();
            startButton.setText("Stop");
            started = true;
        } else {
            Platform.exit();
        }
    }

    public void startServer() {
        String clientTcpPort = clientTcpPortField.getText().trim();
        String dbLocation = dbLocationField.getText().trim();

        String regPortText = regPortField.getText().trim();
        String rmiBackupServiceName = rmiBackupServiceNameField.getText().trim();

        if (clientTcpPort.isEmpty() || regPortText.isEmpty() || dbLocation.isEmpty() || rmiBackupServiceName.isEmpty()) {
            System.out.println("[ServerController] Information missing to start de server!");
            showError("Information missing to start de server!");
            started = false;
            return;
        }

        this.server = new Server();

        try {
            int regPort = Integer.parseInt(regPortText);
            Runnable runnable = () -> server.initServer(clientTcpPort, dbLocation, regPort, rmiBackupServiceName, this);

            serverThread = new Thread(runnable);
            serverThread.setDaemon(true);
            serverThread.start();
        } catch (NumberFormatException e) {
            System.out.println("[ServerController] Error parsing reg port");
            showError("[ServerController] Error parsing reg port");
        }
    }

    public void addToConsole(String message) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        Platform.runLater(() -> consoleTextArea.appendText(formatter.format(date) + " - " + message + "\n"));
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setText("");
    }
}
