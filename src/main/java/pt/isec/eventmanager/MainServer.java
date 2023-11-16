package pt.isec.eventmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.isec.eventmanager.server.Server;
import pt.isec.eventmanager.server.ServerController;

import java.io.IOException;

public class MainServer extends Application {
    Server server;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainClient.class.getResource("fxml/server.fxml"));

        Platform.runLater(() -> {
            this.server = new Server();
            ServerController serverController = fxmlLoader.getController();
            serverController.initServerController(server);
        });

        Scene scene = new Scene(fxmlLoader.load(), 800, 800);
        stage.setTitle("EventManager Server");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
