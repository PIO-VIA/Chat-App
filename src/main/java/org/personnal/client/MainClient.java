package org.personnal.client;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.personnal.client.UI.ChatView;
import org.personnal.client.UI.LoginView;
import org.personnal.client.UI.RegisterView;
import org.personnal.client.controller.ChatController;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.RequestType;

import java.util.List;
import java.util.function.Consumer;

import java.io.IOException;


public class MainClient extends Application {

    private Stage primaryStage;
    private StackPane root;
    private ClientSocketManager socketManager;
    private Consumer<String[]> onUsersUpdated;
    private Timeline refreshTimeline;

    public void initializeAutoRefresh() {
        socketManager.setUsersUpdateListener(this::handleUsersUpdate);

        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> requestUsers())
        );
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    private void handleUsersUpdate(List<String> users) {
        Platform.runLater(() -> {
            if (onUsersUpdated != null) {
                onUsersUpdated.accept(users.toArray(new String[0]));
            }
        });
    }

    private void requestUsers() {
        try {
            PeerRequest request = new PeerRequest(RequestType.GET_CONNECTED_USERS, null);
            socketManager.sendRequest(request);
        } catch (IOException e) {
            System.err.println("Erreur lors de la requête des utilisateurs: " + e.getMessage());
        }
    }

    public void setOnUsersUpdated(Consumer<String[]> onUsersUpdated) {
        this.onUsersUpdated = onUsersUpdated;
    }

    public void notifyUserListUpdate(String[] users) {
        if (onUsersUpdated != null) {
            onUsersUpdated.accept(users);
        }
    }

    // Exemple : simulateur d'une mise à jour reçue du serveur
    public void receiveUserListFromServer(String[] users) {
        System.out.println("Mise à jour des utilisateurs reçue : " + String.join(", ", users));
        notifyUserListUpdate(users); // Notifie le contrôleur
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.socketManager = ClientSocketManager.getInstance();
        this.primaryStage = primaryStage;
        this.root = new StackPane();
        initializeAutoRefresh();

        // Définir la taille de la fenêtre de connexion/inscription
        Scene scene = new Scene(root, 800, 500);

        // Configurer la fenêtre principale
        primaryStage.setTitle("Alanya - Connexion");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true); // Permettre le redimensionnement
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);

        // Centrer la fenêtre sur l'écran
        primaryStage.centerOnScreen();

        // Afficher la vue de login par défaut
        showLoginView();

        primaryStage.show();
    }

    public void showLoginView() {
        LoginView loginView = new LoginView(this, socketManager);
        root.getChildren().setAll(loginView.getView());
        primaryStage.setTitle("Alanya - Connexion");

        // Maintenir la taille actuelle sans transitions forcées
        if (primaryStage.getScene() != null && primaryStage.getScene().getWindow() != null) {
            // S'assurer seulement que la fenêtre respecte les dimensions minimales
            double width = Math.max(primaryStage.getWidth(), 700);
            double height = Math.max(primaryStage.getHeight(), 500);
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
        }
    }

    public void showRegisterView() {
        RegisterView registerView = new RegisterView(this, socketManager);
        root.getChildren().setAll(registerView.getView());
        primaryStage.setTitle("Alanya - Inscription");

        // Maintenir la taille actuelle sans transitions forcées
        if (primaryStage.getScene() != null && primaryStage.getScene().getWindow() != null) {
            // S'assurer seulement que la fenêtre respecte les dimensions minimales
            double width = Math.max(primaryStage.getWidth(), 700);
            double height = Math.max(primaryStage.getHeight(), 500);
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
        }
    }

    public void showChatView(String username) {
        ChatController chatController = new ChatController(this, username);
        ChatView chatView = new ChatView(chatController);
        Scene chatScene = new Scene(chatView.getView(), 1000, 600);
        primaryStage.setScene(chatScene);
        primaryStage.setTitle("💬 Alanya - Chat (" + username + ")");
        primaryStage.setResizable(true); // Permettre le redimensionnement pour la vue de chat
        primaryStage.centerOnScreen();
    }

    @Override
    public void stop() {
        // Arrêter le timeline de rafraîchissement quand l'application se ferme
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }

        // Fermer proprement la connexion au serveur
        if (socketManager != null) {
            socketManager.closeConnection();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}