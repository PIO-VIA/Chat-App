package org.personnal.client;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
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
    private ChatController currentChatController;

    public void initializeAutoRefresh() {
        try {
            socketManager = ClientSocketManager.getInstance();
            socketManager.setUsersUpdateListener(this::handleUsersUpdate);

            refreshTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(5), e -> requestUsers())
            );
            refreshTimeline.setCycleCount(Animation.INDEFINITE);
            refreshTimeline.play();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'initialisation du rafraîchissement automatique: " + e.getMessage());
        }
    }

    private void handleUsersUpdate(List<String> users) {
        Platform.runLater(() -> {
            if (onUsersUpdated != null) {
                System.out.println("Mise à jour de la liste des utilisateurs: " + users);
                onUsersUpdated.accept(users.toArray(new String[0]));
            }
        });
    }

    private void requestUsers() {
        try {
            System.out.println("Demande de la liste des utilisateurs...");
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

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
        this.root = new StackPane();

        try {
            this.socketManager = ClientSocketManager.getInstance();
            initializeAutoRefresh();
        } catch (IOException e) {
            System.err.println("Erreur de connexion au serveur: " + e.getMessage());
            // On continue malgré l'erreur, pour permettre à l'utilisateur de se connecter plus tard
        }

        // Définir la taille de la fenêtre de connexion/inscription
        Scene scene = new Scene(root, 800, 500);

        // Configurer la fenêtre principale
        primaryStage.setTitle("Alanya - Connexion");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
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
        // Créer et stocker la référence au contrôleur de chat
        currentChatController = new ChatController(this, username);
        ChatView chatView = new ChatView(currentChatController);

        Scene chatScene = new Scene(chatView.getView(), 1000, 600);
        primaryStage.setScene(chatScene);
        primaryStage.setTitle("💬 Alanya - Chat (" + username + ")");
        primaryStage.setResizable(true); // Permettre le redimensionnement pour la vue de chat
        primaryStage.centerOnScreen();

        // Demander immédiatement la liste des utilisateurs après l'affichage de la vue de chat
        Platform.runLater(this::requestUsers);
    }

    @Override
    public void stop() {
        System.out.println("Fermeture de l'application...");
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