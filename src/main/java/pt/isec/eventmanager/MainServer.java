package pt.isec.eventmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.isec.eventmanager.server.ServerController;

import java.io.IOException;

public class MainServer extends Application {
    ServerController serverController;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainClient.class.getResource("fxml/server.fxml"));


        Scene scene = new Scene(fxmlLoader.load(), 1000, 800);
        stage.setTitle("EventManager Server");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> {
            this.serverController = fxmlLoader.getController();
        });

        stage.setOnCloseRequest(event -> {
            serverController.stopServer();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
