package org.personnal.client;

import javafx.application.Platform;
import org.personnal.client.network.ClientConnection;
import org.personnal.client.network.EventDispatcher;

import java.io.IOException;
import java.net.Socket;


import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.*;

public class MainClass extends Application {
    private TextArea messageArea;
    private BufferedWriter output;
    private BufferedReader input;
    private Socket socket;
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showLoginScreen();
    }

    private void showLoginScreen() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(15);
        grid.setHgap(15);

        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Button loginBtn = new Button("Login");
        Button registerBtn = new Button("Register");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(loginBtn, 0, 2);
        grid.add(registerBtn, 1, 2);

        loginBtn.setOnAction(e -> handleAuth(usernameField.getText(), passwordField.getText(), true));
        registerBtn.setOnAction(e -> handleAuth(usernameField.getText(), passwordField.getText(), false));

        Scene scene = new Scene(grid, 300, 200);
        primaryStage.setTitle("Authentification");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleAuth(String username, String password, boolean isLogin) {
        try {
            ClientConnection connection = new ClientConnection("localhost", 5000);
            socket = connection.getClientSocket();
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            EventDispatcher.username = username;
            EventDispatcher.password = password;

            if (isLogin) {
                if (EventDispatcher.handleLogin(output, input)) {
                    primaryStage.close();
                    showChatInterface();
                } else {
                    showAlert("Erreur", "Login échoué !");
                }
            } else {
                EventDispatcher.handleRegister(output, input);
                showAlert("Succès", "Enregistrement réussi !");
            }
        } catch (IOException ex) {
            showAlert("Erreur", ex.getMessage());
        }
    }

    private void showChatInterface() {
        Stage chatStage = new Stage();
        BorderPane root = new BorderPane();

        messageArea = new TextArea();
        messageArea.setEditable(false);
        root.setCenter(new ScrollPane(messageArea));

        TextField receiverField = new TextField();
        receiverField.setPromptText("Destinataire");
        TextField messageField = new TextField();
        messageField.setPromptText("Message");
        Button sendBtn = new Button("Envoyer");

        HBox inputBox = new HBox(10);
        inputBox.getChildren().addAll(receiverField, messageField, sendBtn);
        inputBox.setPadding(new Insets(15));
        root.setBottom(inputBox);

        sendBtn.setOnAction(e -> {
            try {
                EventDispatcher.sendMessage(
                        receiverField.getText(),
                        messageField.getText(),
                        output
                );
                messageField.clear();
            } catch (IOException ex) {
                showAlert("Erreur", "Erreur d'envoi: " + ex.getMessage());
            }
        });

        chatStage.setOnCloseRequest(e -> {
            try {
                EventDispatcher.disconnect(output, input);
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        EventDispatcher.startSession(output, input, messageArea);

        chatStage.setScene(new Scene(root, 600, 400));
        chatStage.setTitle("Chat - " + EventDispatcher.username);
        chatStage.show();
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
