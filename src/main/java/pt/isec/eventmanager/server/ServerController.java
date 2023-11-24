package pt.isec.eventmanager.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import pt.isec.eventmanager.rmi.ServerServiceObserverInterface;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ServerController {
    @FXML
    public ListView<ServerServiceObserverInterface> backupServersListView;
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
    private TextArea heartBeatTextArea;
    @FXML
    private Button startButton;
    @FXML
    private Label errorLabel;
    @FXML
    private Label dbVersionLabel;
    @FXML
    private Circle rmiServiceCircle;
    @FXML
    private Circle heartBeatServiceCircle;

    private Server server;
    private Thread mainServerThread;
    private boolean started = false;

    @FXML
    private void initialize() {
        //TODO: Remove after testing
        clientTcpPortField.setText("6000");
        regPortField.setText("1099");
        dbLocationField.setText("./DB");
        rmiBackupServiceNameField.setText("eventmanagerbackup");
    }

    @FXML
    private void handleStartButtonAction() {
        hideError();

        if (!started) {
            consoleTextArea.clear();
            heartBeatTextArea.clear();
            addToConsole("Starting server");

            clientTcpPortField.setDisable(true);
            regPortField.setDisable(true);
            dbLocationField.setDisable(true);
            rmiBackupServiceNameField.setDisable(true);

            startButton.setText("Stop");
            startServer();
        } else {
            addToConsole("Shutting down server");

            if (mainServerThread != null && mainServerThread.isAlive()) {
                server.stopServer();
                mainServerThread.interrupt();
            }

            clientTcpPortField.setDisable(false);
            regPortField.setDisable(false);
            dbLocationField.setDisable(false);
            rmiBackupServiceNameField.setDisable(false);

            startButton.setText("Start");
            started = false;
        }
    }

    private void startServer() {
        String clientTcpPort = clientTcpPortField.getText().trim();
        String dbLocation = dbLocationField.getText().trim();
        String regPortText = regPortField.getText().trim();
        String rmiBackupServiceName = rmiBackupServiceNameField.getText().trim();

        if (clientTcpPort.isEmpty() || regPortText.isEmpty() || dbLocation.isEmpty() || rmiBackupServiceName.isEmpty()) {
            System.out.println("[ServerController] Information missing to start de server!");
            showError("Information missing to start de server!");
            return;
        }

        this.server = new Server();
        started = true;

        try {
            int regPort = Integer.parseInt(regPortText);

            Runnable runnable = () -> server.initServer(dbLocation, rmiBackupServiceName, regPort, clientTcpPort, this);

            mainServerThread = new Thread(runnable);
            mainServerThread.setDaemon(true);
            mainServerThread.start();
        } catch (NumberFormatException e) {
            System.out.println("[ServerController] Information missing to start de server!");
            showError("Information missing to start de server!");
        }
    }

    public void stopServer() {
        if (server != null)
            server.stopServer();
    }

    public void addToConsole(String message) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        Platform.runLater(() -> consoleTextArea.appendText(formatter.format(date) + " - " + message + "\n"));
    }

    public void addToHeartBeatConsole(String message) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        Platform.runLater(() -> heartBeatTextArea.appendText(formatter.format(date) + " - " + message + "\n"));
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setText("");
    }

    public void setRMIServiceOnline(boolean online) {
        Platform.runLater(() -> {
            if (online) {
                rmiServiceCircle.setFill(Color.GREEN);
            } else {
                rmiServiceCircle.setFill(Color.RED);
            }
        });
    }

    public void setHeartBeatServiceOnline(boolean online) {
        Platform.runLater(() -> {
            if (online) {
                heartBeatServiceCircle.setFill(Color.GREEN);
            } else {
                heartBeatServiceCircle.setFill(Color.RED);
            }
        });
    }

    public void initObserversListView(List<ServerServiceObserverInterface> observers) {
        Platform.runLater(() -> {
            backupServersListView.getItems().clear();
            backupServersListView.getItems().addAll(observers);
        });
    }

    public void setDbVersionLabel(int dbVersion) {
        String dbVersionString = String.valueOf(dbVersion);

        Platform.runLater(() -> dbVersionLabel.setText(dbVersionString));
    }
}
