package com.example.whatsapp;

import java.io.*;
import java.net.URL;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Server extends Application {
    private final double longueur =840;
    private final double largeur =580;

    @Override
    public void start(Stage stage) throws IOException {

        Scene scene = new Scene(new Controlserver());
        URL cssURL = getClass().getResource("style.css");
        if (cssURL == null) {
            System.out.println("Fichier CSS introuvable !");
        } else {
            System.out.println("Fichier CSS chargé : " + cssURL);
            scene.getStylesheets().add(cssURL.toExternalForm());
        }
        stage.setTitle("ANALYA");
        stage.setHeight(largeur);
        stage.setWidth(longueur);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {

        launch();


    }
}
