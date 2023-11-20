package pt.isec.eventmanager.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import pt.isec.eventmanager.users.User;
import pt.isec.eventmanager.util.LabelType;

public class EditUserController {
    @FXML
    private TextField emailField;
    @FXML
    private TextField nameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private Button saveButton;
    @FXML
    private TextField studentNumberField;

    private Client client;
    private ClientAuthenticatedController parentController;

    @FXML
    public void initialize() {
    }

    @FXML
    void handleSaveButtonAction() {
        if (client == null || parentController == null)
            return;

        String password = passwordField.getText().trim();
        String newPassword = newPasswordField.getText().trim();
        String name = nameField.getText().trim();
        String studentNumber = studentNumberField.getText().trim();

        if (password.isEmpty() || newPassword.isEmpty() || name.isEmpty() || studentNumber.isEmpty()) {
            parentController.showInfo("Missing elements to edit user!", LabelType.ERROR);
            return;
        }

        if (!password.equals(newPassword)) {
            parentController.showInfo("Passwords must match", LabelType.ERROR);
            return;
        }

        User user = new User(client.getUser().getEmail(), password, name, studentNumber, false);

        Thread thread = new Thread(() -> {
            boolean success = client.editUser(user);

            Platform.runLater(() -> {
                if (success) {
                    parentController.initLayout();
                    parentController.showInfo("Operation successfully", LabelType.INFO);
                } else {
                    parentController.showInfo("Operation Error!", LabelType.ERROR);
                }
            });
        });
        thread.start();
    }

    public void initEditUserController(Client client, ClientAuthenticatedController controller) {
        this.client = client;
        this.parentController = controller;
        initEditUserFields();
    }

    public void initEditUserFields() {
        if (client == null || parentController == null)
            return;

        emailField.setText(client.getUser().getEmail());
        nameField.setText(client.getUser().getName());
        studentNumberField.setText(client.getUser().getName());
        passwordField.setText(client.getUser().getPassword());
        newPasswordField.setText(client.getUser().getPassword());
    }
}
