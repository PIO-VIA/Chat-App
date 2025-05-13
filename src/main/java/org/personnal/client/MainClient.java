package org.personnal.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.personnal.client.UI.ChatView;
import org.personnal.client.UI.LoginView;
import org.personnal.client.UI.RegisterView;
import org.personnal.client.controller.ChatController;
import org.personnal.client.network.ClientSocketManager;

import java.io.IOException;


public class MainClient extends Application {

    private Stage primaryStage;
    private StackPane root;
    private ClientSocketManager socketManager;
    @Override
    public void start(Stage primaryStage) throws IOException {
        this.socketManager = ClientSocketManager.getInstance();
        this.primaryStage = primaryStage;
        this.root = new StackPane();

        // Afficher la vue de login par défaut
        showLoginView();

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("Alanya - Connexion");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void showLoginView() {
        LoginView loginView = new LoginView(this, socketManager);
        root.getChildren().setAll(loginView.getView());
    }

    public void showRegisterView() {
        RegisterView registerView = new RegisterView(this,socketManager);
        root.getChildren().setAll(registerView.getView());

    }
    public void showChatView(String username) {
        ChatController chatController = new ChatController(this, username);
        ChatView chatView = new ChatView(chatController);
        Scene chatScene = new Scene(chatView.getView(), 1000, 600);
        primaryStage.setScene(chatScene);
        primaryStage.setTitle("💬 Alanya - Chat");
        primaryStage.centerOnScreen();
    }

    public static void main(String[] args) throws IOException {
        launch(args);

    }
}
