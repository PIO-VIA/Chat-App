package org.personnal.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.personnal.client.UI.ChatView;
import org.personnal.client.UI.LoginView;
import org.personnal.client.UI.RegisterView;
import org.personnal.client.controller.ChatController;
import org.personnal.client.controller.LoginController;
import org.personnal.client.controller.RegisterController;
import org.personnal.client.network.ClientSocketManager;

import java.io.IOException;

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
        this.socketManager = ClientSocketManager.getInstance();
        this.primaryStage = primaryStage;
        this.root = new StackPane();


        // Configuration de la fenêtre principale
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setResizable(true);

        // Icône de l'application (à implémenter si vous avez une icône)
        // primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));

        // Afficher la vue de login par défaut
        showLoginView();

        Scene scene = new Scene(root, PREF_WIDTH, PREF_HEIGHT);

        // Appliquer CSS global (à créer dans un fichier séparé)
        // scene.getStylesheets().add(getClass().getResource("/styles/global.css").toExternalForm());

        primaryStage.setTitle("Alanya - Connexion");
        primaryStage.setScene(scene);
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
    }

    public void showRegisterView() {
        RegisterController registerController = new RegisterController(this, socketManager);
        RegisterView registerView = new RegisterView(this, registerController);
        root.getChildren().setAll(registerView.getView());
        primaryStage.setTitle("Alanya - Inscription");
    }

    public void showChatView(String username) throws IOException {
        ChatController chatController = new ChatController(this, username);
        ChatView chatView = new ChatView(chatController);

        // Animation de transition (optional - implémentation future)
        // FadeTransition ft = new FadeTransition(Duration.millis(300), chatView.getView());
        // ft.setFromValue(0.0);
        // ft.setToValue(1.0);
        // ft.play();

        Scene chatScene = new Scene(chatView.getView(), CHAT_WIDTH, CHAT_HEIGHT);

        // Appliquer CSS spécifique au chat (à créer dans un fichier séparé)
        // chatScene.getStylesheets().add(getClass().getResource("/styles/chat.css").toExternalForm());

        primaryStage.setScene(chatScene);
        primaryStage.setTitle("💬 Alanya - Chat (" + username + ")");
        primaryStage.centerOnScreen();
    }

    @Override
    public void stop() {
        // Nettoyer les ressources, fermer les connexions
        try {
            if (socketManager != null) {
                // Méthode à ajouter pour fermer proprement la connexion
                // socketManager.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}