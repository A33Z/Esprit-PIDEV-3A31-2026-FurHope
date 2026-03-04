package com.projet.test;

import com.projet.payment.SpringPaymentServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class mainfx extends Application {
    @Override
    public void init() {
        SpringPaymentServer.start();
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setWidth(1000);
        stage.setHeight(650);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/shop.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setTitle("Ajouter Demande");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        SpringPaymentServer.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
