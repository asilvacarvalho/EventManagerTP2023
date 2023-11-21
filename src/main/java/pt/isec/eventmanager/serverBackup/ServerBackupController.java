package pt.isec.eventmanager.serverBackup;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ServerBackupController {
    @FXML
    private TextField dbLocationField;
    @FXML
    private TextArea consoleTextArea;
    @FXML
    private Button startButton;
    @FXML
    private Label errorLabel;

    private boolean started = false;

    private ServerBackup serverBackup;

    @FXML
    private void initialize() {
        //TODO: Remove after testing
        dbLocationField.setText("./DBBackup");
    }

    @FXML
    private void handleStartButtonAction() {
        if (!started) {
            boolean success = checkDBDirectory();

            if (success) {
                addToConsole("Server Backup started");
                started = true;
                startButton.setText("Stop");
                this.serverBackup = new ServerBackup();
                serverBackup.startHeartBeatLookup(this);
            }
        } else {
            serverBackup.stopHeartBeatLookup();
            addToConsole("Server Backup stoped");
            started = false;
            startButton.setText("Start");
        }
    }

    public void initServerBackupController() {
    }

    private boolean checkDBDirectory() {
        hideError();

        if (dbLocationField.getText().isEmpty()) {
            showError("Insert a DB location");
            return false;
        }

        String dbLocation = dbLocationField.getText().trim();
        File dbDirectory = new File(dbLocation);

        if (dbDirectory.exists()) {
            if (dbDirectory.isDirectory()) {
                if (Objects.requireNonNull(dbDirectory.list()).length > 0) {
                    showError("The DB location is not empty, plz insert new location");
                    return false;
                }
            } else {
                showError("Incorrect DB location");
                return false;
            }
        } else {
            showError("The DB location is not a directory");
            return false;
        }

        return true;
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
