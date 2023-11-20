package pt.isec.eventmanager.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import pt.isec.eventmanager.users.UserKey;
import pt.isec.eventmanager.util.LabelType;

public class UserKeyController {
    @FXML
    public TextField userKeyTextField;

    private ClientAuthenticatedController parentController;
    private Client client;

    @FXML
    public void initialize() {
    }

    @FXML
    public void handleSaveEvent() {
        if (userKeyTextField.getText().isEmpty()) return;

        try {
            int userKey = Integer.parseInt(userKeyTextField.getText());
            UserKey newUserKey = new UserKey(client.getUser().getEmail(), userKey);

            Thread thread = new Thread(() -> {
                boolean success = client.insertUserKey(newUserKey);

                Platform.runLater(() -> {
                    if (success) {
                        parentController.showInfo("Operation successfully", LabelType.INFO);
                        parentController.clearMainContentArea();
                    } else {
                        parentController.showInfo("Operation Error!", LabelType.ERROR);
                    }
                });
            });
            thread.start();
        } catch (Exception e) {
            System.out.println("[UserKeyController] Error inserting user key " + e.getMessage());
            parentController.showInfo("Operation Error!", LabelType.ERROR);
        }
    }

    public void initUserKeyController(Client client, ClientAuthenticatedController controller) {
        this.parentController = controller;
        this.client = client;
    }
}
