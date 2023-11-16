package pt.isec.eventmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.isec.eventmanager.client.Client;
import pt.isec.eventmanager.client.ClientController;

import java.io.IOException;

public class MainClient extends Application {
    Stage mainStage;
    Client client;

    @Override
    public void start(Stage stage) throws IOException {
        mainStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(MainClient.class.getResource("fxml/client.fxml"));

        Platform.runLater(() -> {
            this.client = new Client();
            ClientController clientController = fxmlLoader.getController();
            clientController.initClientController(mainStage, client);
        });

        Scene scene = new Scene(fxmlLoader.load(), 1000, 800);
        stage.setTitle("EventManager Client");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}