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

public class RegisterView {

    private final MainClient app;
    private final VBox layout;
    private final ClientSocketManager socketManager;

    public RegisterView(MainClient app, ClientSocketManager socketManager) {
        this.app = app;
        this.socketManager = socketManager;
        this.layout = new VBox(15);
        initUI();
    }

    private void initUI() {
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #f0f0ff;");

        Label title = new Label("📝 Inscription");
        title.setFont(Font.font(22));
        title.setTextFill(Color.web("#333"));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");

        Button registerBtn = new Button("Créer un compte");
        registerBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Erreur", "Veuillez remplir tous les champs.");
                return;
            }

            try {
                PeerRequest request = new PeerRequest(RequestType.REGISTER, Map.of(
                        "username", username,
                        "password", password
                ));
                socketManager.sendRequest(request);

                PeerResponse response = socketManager.readResponse();

                if (response.isSuccess()) {
                    showAlert("Succès", "Compte créé avec succès !");
                    app.showLoginView(); // Revenir à la vue de connexion
                } else {
                    showAlert("Échec de l'inscription", response.getMessage());
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de communiquer avec le serveur.");
            }
        });

        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

        Label switchToLogin = new Label("Déjà inscrit ? Connectez-vous");
        switchToLogin.setStyle("-fx-text-fill: #0077cc; -fx-underline: true;");
        switchToLogin.setOnMouseClicked(e -> app.showLoginView());

        layout.getChildren().addAll(title, usernameField, passwordField, registerBtn, switchToLogin);
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
