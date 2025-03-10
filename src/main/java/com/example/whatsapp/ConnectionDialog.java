package com.example.whatsapp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class ConnectionDialog extends Application {
    private TextField hostField; // Champ pour l'adresse IP
    private TextField portField; // Champ pour le port
    private Button connectButton; // Bouton pour se connecter
    private SocketManager socketManager; // Gestionnaire de socket


    // Constructeur
    public ConnectionDialog(SocketManager socketManager) {
        this.socketManager = socketManager;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Connexion Client");

        // Création des composants
        Label hostLabel = new Label("Adresse IP :");
        hostField = new TextField("localhost"); // Valeur par défaut
        Label portLabel = new Label("Port :");
        portField = new TextField("12345"); // Valeur par défaut
        connectButton = new Button("Se connecter");

        // Gestion des événements
        connectButton.setOnAction(e -> {
            String host = hostField.getText(); // Récupère l'adresse IP
            int port = Integer.parseInt(portField.getText()); // Récupère le port
             socketManager.connectToServer(host, port);// Établit la connexion
            // Démarrer les threads de réception

            primaryStage.close(); // Ferme la fenêtre

        });

        // Mise en page
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(10);
        grid.setHgap(10);
        grid.add(hostLabel, 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(portLabel, 0, 1);
        grid.add(portField, 1, 1);
        grid.add(connectButton, 0, 2);

        // Affichage de la fenêtre
        Scene scene = new Scene(grid, 300, 150);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}