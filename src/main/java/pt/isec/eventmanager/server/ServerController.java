package pt.isec.eventmanager.server;

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


    @FXML
    private void initialize() {
        //TODO: Remove after testing
        clientTcpPortField.setText("6000");
        regPortField.setText("5000");
        dbLocationField.setText("./DB/eventmanagerdb.sqlite");
        rmiBackupServiceNameField.setText("eventmanagerbackup");
    }

    public void initServerController(Server server) {
        this.server = server;
    }

    @FXML
    private void handleStartButtonAction() {
        hideError();

        String clientTcpPort = clientTcpPortField.getText().trim();
        String regPort = regPortField.getText().trim();
        String dbLocation = dbLocationField.getText().trim();
        String rmiBackupServiceName = rmiBackupServiceNameField.getText().trim();

        if (clientTcpPort.isEmpty() || regPort.isEmpty() || dbLocation.isEmpty() || rmiBackupServiceName.isEmpty()) {
            System.out.println("[ServerController] Information missing to start de server!");
            showError("Information missing to start de server!");
            return;
        }

        Runnable runnable = () -> {
            server.initServer(clientTcpPort, dbLocation, this);
        };

        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }

    public void addToConsole(String message) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        consoleTextArea.appendText(formatter.format(date) + " - " + message + "\n");
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
