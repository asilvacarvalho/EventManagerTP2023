package pt.isec.eventmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.isec.eventmanager.serverBackup.ServerBackupController;

import java.io.IOException;

public class MainServerBackup extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainClient.class.getResource("fxml/server-backup.fxml"));

        Platform.runLater(() -> {
            ServerBackupController serverController = fxmlLoader.getController();
            serverController.initServerBackupController();
        });

        Scene scene = new Scene(fxmlLoader.load(), 800, 800);
        stage.setTitle("EventManager Server Backup");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
