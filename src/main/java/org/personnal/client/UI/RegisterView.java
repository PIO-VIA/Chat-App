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

public class RegisterView {

    private final MainClient app;
    private final StackPane layout;
    private final ClientSocketManager socketManager;

    public RegisterView(MainClient app, ClientSocketManager socketManager) {
        this.app = app;
        this.socketManager = socketManager;
        this.layout = new StackPane();
        initUI();
    }

    private void initUI() {
        // Fond bleu pour l'ensemble de l'écran
        layout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a6fc7, #2e8ede);");

        // Création du cadre blanc central
        VBox registerBox = new VBox(20);
        registerBox.setPadding(new Insets(30));
        registerBox.setMaxWidth(350);
        registerBox.setAlignment(Pos.CENTER);
        registerBox.setStyle("-fx-background-color: white; -fx-background-radius: 10px;");

        // Effet d'ombre pour le cadre
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10.0);
        dropShadow.setOffsetX(0);
        dropShadow.setOffsetY(0);
        dropShadow.setColor(Color.color(0, 0, 0, 0.3));
        registerBox.setEffect(dropShadow);

        // Titre de l'application
        Label appTitle = new Label("ALANYA");
        appTitle.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        appTitle.setTextFill(Color.web("#1a6fc7"));

        // Sous-titre
        Label subtitle = new Label("Créer un compte");
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

        // Confirmer le mot de passe (champ supplémentaire pour l'inscription)
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le mot de passe");
        confirmPasswordField.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px; -fx-border-color: #ddd; -fx-border-radius: 5px; -fx-padding: 10px;");

        // Bouton d'inscription
        Button registerBtn = new Button("S'INSCRIRE");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setStyle(
                "-fx-background-color: #1a6fc7; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px; " +
                        "-fx-background-radius: 5px;"
        );

        // Effet hover sur le bouton
        registerBtn.setOnMouseEntered(e -> registerBtn.setStyle(
                "-fx-background-color: #0d5fb7; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px; " +
                        "-fx-background-radius: 5px;"
        ));

        registerBtn.setOnMouseExited(e -> registerBtn.setStyle(
                "-fx-background-color: #1a6fc7; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px; " +
                        "-fx-background-radius: 5px;"
        ));

        // Action du bouton d'inscription
        registerBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showAlert("Erreur", "Veuillez remplir tous les champs.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showAlert("Erreur", "Les mots de passe ne correspondent pas.");
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
                    app.showLoginView();
                } else {
                    showAlert("Échec de l'inscription", response.getMessage());
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de communiquer avec le serveur.");
            }
        });

        // Lien pour se connecter
        Label switchToLogin = new Label("Déjà inscrit ? Connectez-vous");
        switchToLogin.setStyle("-fx-text-fill: #1a6fc7; -fx-cursor: hand;");
        switchToLogin.setOnMouseEntered(e -> switchToLogin.setStyle("-fx-text-fill: #0d5fb7; -fx-cursor: hand; -fx-underline: true;"));
        switchToLogin.setOnMouseExited(e -> switchToLogin.setStyle("-fx-text-fill: #1a6fc7; -fx-cursor: hand;"));
        switchToLogin.setOnMouseClicked(e -> app.showLoginView());

        // Ajout des éléments dans le conteneur
        registerBox.getChildren().addAll(appTitle, subtitle, usernameField, passwordField, confirmPasswordField, registerBtn, switchToLogin);

        // Ajout du conteneur à la mise en page principale
        layout.getChildren().add(registerBox);
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