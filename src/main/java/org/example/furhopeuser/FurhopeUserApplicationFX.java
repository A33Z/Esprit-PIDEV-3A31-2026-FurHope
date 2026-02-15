package org.example.furhopeuser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FurhopeUserApplicationFX extends Application{
    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/Welcome.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 700);

        stage.setTitle("FurHope - Animal Shelter Platform");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
