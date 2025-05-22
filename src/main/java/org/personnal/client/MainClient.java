package org.personnal.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.personnal.client.DatabaseInitializer;
import org.personnal.client.UI.ChatView;
import org.personnal.client.UI.LoginView;
import org.personnal.client.UI.RegisterView;
import org.personnal.client.controller.ChatController;
import org.personnal.client.controller.LoginController;
import org.personnal.client.controller.RegisterController;
import org.personnal.client.network.ClientSocketManager;

import java.io.IOException;
import java.net.URL;

public class MainClient extends Application {

    private Stage primaryStage;
    private StackPane root;
    private ClientSocketManager socketManager;
    private final double MIN_WIDTH = 600;
    private final double MIN_HEIGHT = 500;
    private final double PREF_WIDTH = 450;
    private final double PREF_HEIGHT = 550;
    private final double CHAT_WIDTH = 1000;
    private final double CHAT_HEIGHT = 700;

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Initialiser la base de données locale
        DatabaseInitializer.initialize();

        this.socketManager = ClientSocketManager.getInstance();
        this.primaryStage = primaryStage;
        this.root = new StackPane();

        // Configuration de la fenêtre principale
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setResizable(true);

        // Créer la scène AVANT d'appeler showLoginView()
        Scene scene = new Scene(root, PREF_WIDTH, PREF_HEIGHT);

        // Appliquer les CSS globaux
        URL globalCssUrl = getClass().getResource("/styles/global.css");
        URL fontsCssUrl = getClass().getResource("/styles/fonts.css");
        URL dialogsCssUrl = getClass().getResource("/styles/dialogs.css");

        if (globalCssUrl != null) {
            scene.getStylesheets().add(globalCssUrl.toExternalForm());
        } else {
            System.err.println("ATTENTION: Le fichier CSS global n'a pas pu être chargé");
        }

        if (fontsCssUrl != null) {
            scene.getStylesheets().add(fontsCssUrl.toExternalForm());
        } else {
            System.err.println("ATTENTION: Le fichier CSS des polices n'a pas pu être chargé");
        }

        if (dialogsCssUrl != null) {
            scene.getStylesheets().add(dialogsCssUrl.toExternalForm());
        } else {
            System.err.println("ATTENTION: Le fichier CSS des dialogues n'a pas pu être chargé");
        }

        // Définir la scène sur le stage AVANT d'appeler showLoginView()
        primaryStage.setScene(scene);
        primaryStage.setTitle("Alanya - Connexion");

        // Maintenant appeler showLoginView()
        showLoginView();

        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public void showLoginView() {
        LoginController loginController = new LoginController(this, socketManager);
        LoginView loginView = new LoginView(this, loginController);
        root.getChildren().setAll(loginView.getView());
        primaryStage.setTitle("Alanya - Connexion");
        primaryStage.setWidth(PREF_WIDTH);
        primaryStage.setHeight(PREF_HEIGHT);
        primaryStage.centerOnScreen();

        // Appliquer le CSS d'authentification à la scène actuelle
        Scene currentScene = primaryStage.getScene();
        if (currentScene != null) {
            // Supprimer les CSS précédents sauf le global
           // currentScene.getStylesheets().clear();

            // Ajouter les CSS appropriés
            URL globalCssUrl = getClass().getResource("/styles/global.css");
            URL authCssUrl = getClass().getResource("/styles/auth.css");
            URL fontsCssUrl = getClass().getResource("/styles/fonts.css");
            URL dialogsCssUrl = getClass().getResource("/styles/dialogs.css");

            if (globalCssUrl != null) {
                currentScene.getStylesheets().add(globalCssUrl.toExternalForm());
            }

            if (authCssUrl != null) {
                currentScene.getStylesheets().add(authCssUrl.toExternalForm());
            }

            if (fontsCssUrl != null) {
                currentScene.getStylesheets().add(fontsCssUrl.toExternalForm());
            }

            if (dialogsCssUrl != null) {
                currentScene.getStylesheets().add(dialogsCssUrl.toExternalForm());
            }
        }
    }

    public void showRegisterView() {
        RegisterController registerController = new RegisterController(this, socketManager);
        RegisterView registerView = new RegisterView(this, registerController);
        root.getChildren().setAll(registerView.getView());
        primaryStage.setTitle("Alanya - Inscription");

        // Le CSS d'authentification est déjà chargé depuis showLoginView
    }

    public void showChatView(String username) throws IOException {
        ChatController chatController = new ChatController(this, username);
        ChatView chatView = new ChatView(chatController);

        // Créer une nouvelle scène pour le chat
        Scene chatScene = new Scene(chatView.getView(), CHAT_WIDTH, CHAT_HEIGHT);

        // Appliquer les CSS pour l'interface de chat
        URL globalCssUrl = getClass().getResource("/styles/global.css");
        URL chatCssUrl = getClass().getResource("/styles/chat.css");

        if (globalCssUrl != null) {
            chatScene.getStylesheets().add(globalCssUrl.toExternalForm());
        }

        if (chatCssUrl != null) {
            chatScene.getStylesheets().add(chatCssUrl.toExternalForm());
        }

        primaryStage.setScene(chatScene);
        primaryStage.setTitle("💬 Alanya - Chat (" + username + ")");
        primaryStage.centerOnScreen();
    }

    @Override
    public void stop() {
        // Nettoyer les ressources, fermer les connexions
        try {
            if (socketManager != null) {
                socketManager.closeConnection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}