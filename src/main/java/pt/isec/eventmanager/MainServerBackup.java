package pt.isec.eventmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.isec.eventmanager.serverBackup.ServerBackupController;

import java.io.IOException;

public class MainServerBackup extends Application {
    ServerBackupController serverController;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainClient.class.getResource("fxml/server-backup.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 1000, 800);
        stage.setTitle("EventManager Server Backup");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> {
            this.serverController = fxmlLoader.getController();
        });

        stage.setOnCloseRequest(event -> {
            if (serverController != null && serverController.isRunning())
                serverController.stopServer();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
