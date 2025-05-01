package org.personnal.client.UI;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.personnal.client.MainClient;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import java.io.IOException;
import java.util.Map;

public class LoginView {

    private final MainClient app;
    private final VBox layout;
    private final ClientSocketManager socketManager;

    public LoginView(MainClient app, ClientSocketManager socketManager) {
        this.app = app;
        this.socketManager = socketManager;
        this.layout = new VBox(15);
        initUI();
    }

    private void initUI() {
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #f9f9f9;");

        Label title = new Label("🔐 Connexion");
        title.setFont(Font.font(22));
        title.setTextFill(Color.web("#333"));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");

        Button loginBtn = new Button("Se connecter");
        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Erreur", "Veuillez remplir tous les champs.");
                return;
            }

            try {
                PeerRequest request = new PeerRequest(RequestType.LOGIN, Map.of(
                        "username", username,
                        "password", password
                ));
                socketManager.sendRequest(request);

                PeerResponse response = socketManager.readResponse();

                if (response.isSuccess()) {
                    showAlert("Succès", "Connexion réussie !");
                    app.showChatView(usernameField.getText());
                } else {
                    showAlert("Échec de connexion", response.getMessage());
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de communiquer avec le serveur.");
            }
        });


        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");


        Label switchToRegister = new Label("Pas encore de compte ? Cliquez ici");
        switchToRegister.setStyle("-fx-text-fill: #0077cc; -fx-underline: true;");
        switchToRegister.setOnMouseClicked(e -> app.showRegisterView());

        layout.getChildren().addAll(title, usernameField, passwordField, loginBtn, switchToRegister);
    }

    public VBox getView() {
        return layout;
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
