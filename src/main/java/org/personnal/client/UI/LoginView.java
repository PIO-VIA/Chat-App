package org.personnal.client.UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.personnal.client.MainClient;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import java.io.IOException;
import java.util.Map;

public class LoginView {

    private final MainClient app;
    private final BorderPane layout;
    private final ClientSocketManager socketManager;

    public LoginView(MainClient app, ClientSocketManager socketManager) {
        this.app = app;
        this.socketManager = socketManager;
        this.layout = new BorderPane();
        initUI();
    }

    private void initUI() {
        // Fond bleu pour l'ensemble de l'écran
        layout.setStyle("-fx-background-color: linear-gradient(to right, #1a6fc7, #2e8ede);");

        // Création du cadre blanc central (carré)
        VBox loginBox = new VBox(15);
        loginBox.setPadding(new Insets(30));
        loginBox.setMaxWidth(400);
        loginBox.setMaxHeight(400);
        loginBox.setMinWidth(350);
        loginBox.setMinHeight(350);
        loginBox.setPrefWidth(400);
        loginBox.setPrefHeight(400);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setStyle("-fx-background-color: white; -fx-background-radius: 10px;");

        // Effet d'ombre pour le cadre
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10.0);
        dropShadow.setOffsetX(0);
        dropShadow.setOffsetY(0);
        dropShadow.setColor(Color.color(0, 0, 0, 0.3));
        loginBox.setEffect(dropShadow);

        // Titre de l'application
        Label appTitle = new Label("ALANYA");
        appTitle.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        appTitle.setTextFill(Color.web("#1a6fc7"));

        // Sous-titre
        Label subtitle = new Label("Connexion");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        subtitle.setTextFill(Color.web("#555"));

        // Champ nom d'utilisateur
        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");
        usernameField.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px; -fx-border-color: #ddd; -fx-border-radius: 5px; -fx-padding: 10px;");

        // Champ mot de passe
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px; -fx-border-color: #ddd; -fx-border-radius: 5px; -fx-padding: 10px;");

        // Bouton de connexion
        Button loginBtn = new Button("SE CONNECTER");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle(
                "-fx-background-color: #1a6fc7; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px; " +
                        "-fx-background-radius: 5px;"
        );

        // Effet hover sur le bouton
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(
                "-fx-background-color: #0d5fb7; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px; " +
                        "-fx-background-radius: 5px;"
        ));

        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(
                "-fx-background-color: #1a6fc7; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px; " +
                        "-fx-background-radius: 5px;"
        ));

        // Action du bouton de connexion
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

        // Lien pour s'inscrire
        Label switchToRegister = new Label("Pas encore de compte ? Inscrivez-vous");
        switchToRegister.setStyle("-fx-text-fill: #1a6fc7; -fx-cursor: hand;");
        switchToRegister.setOnMouseEntered(e -> switchToRegister.setStyle("-fx-text-fill: #0d5fb7; -fx-cursor: hand; -fx-underline: true;"));
        switchToRegister.setOnMouseExited(e -> switchToRegister.setStyle("-fx-text-fill: #1a6fc7; -fx-cursor: hand;"));
        switchToRegister.setOnMouseClicked(e -> app.showRegisterView());

        // Ajout des éléments dans le conteneur
        loginBox.getChildren().addAll(appTitle, subtitle, usernameField, passwordField, loginBtn, switchToRegister);

        // Centrer le cadre dans la borderpane
        StackPane centeringPane = new StackPane(loginBox);
        centeringPane.setPadding(new Insets(20));
        layout.setCenter(centeringPane);
    }

    public Pane getView() {
        return layout;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style de l'alerte
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        dialogPane.getStyleClass().add("custom-alert");

        alert.showAndWait();
    }
}