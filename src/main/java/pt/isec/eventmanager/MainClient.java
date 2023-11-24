package pt.isec.eventmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.isec.eventmanager.client.ClientController;

import java.io.IOException;

public class MainClient extends Application {
    Stage mainStage;
    ClientController clientController;

    @Override
    public void start(Stage stage) throws IOException {
        mainStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(MainClient.class.getResource("fxml/client.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 1000, 800);
        stage.setTitle("EventManager Client");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> {
            ClientController clientController = fxmlLoader.getController();
            clientController.initClientController(mainStage);
        });

        stage.setOnCloseRequest(event -> {
            if (clientController != null)
                clientController.stopClient();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}