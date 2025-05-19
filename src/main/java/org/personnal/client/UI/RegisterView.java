package org.personnal.client.UI;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.personnal.client.MainClient;
import org.personnal.client.controller.RegisterController;

public class RegisterView {
    private final BorderPane layout;
    private final RegisterController controller;
    private final MainClient app;

    // Composants UI
    private TextField usernameField;
    private TextField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;

    public RegisterView(MainClient app, RegisterController controller) {
        this.app = app;
        this.controller = controller;
        this.layout = new BorderPane();
        initUI();
    }

    private void initUI() {
        // Appliquer la classe de style au layout principal
        layout.getStyleClass().add("auth-background");

        VBox registerBox = createRegisterBox();
        setupCenterLayout(registerBox);
    }

    private VBox createRegisterBox() {
        VBox registerBox = new VBox(15);
        registerBox.getStyleClass().add("auth-box");
        addComponentsToRegisterBox(registerBox);
        return registerBox;
    }

    private void addComponentsToRegisterBox(VBox box) {
        Label appTitle = new Label("ALANYA");
        appTitle.getStyleClass().add("app-title");

        Label subtitle = new Label("Créer un compte");
        subtitle.getStyleClass().add("app-subtitle");

        usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");
        usernameField.getStyleClass().add("auth-text-field");

        emailField = new TextField();
        emailField.setPromptText("example@gmail.com");
        emailField.getStyleClass().add("auth-text-field");

        passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.getStyleClass().add("auth-text-field");

        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le mot de passe");
        confirmPasswordField.getStyleClass().add("auth-text-field");

        Button registerBtn = createRegisterButton();
        Hyperlink loginLink = createLoginLink();

        box.getChildren().addAll(appTitle, subtitle, usernameField, emailField,
                passwordField, confirmPasswordField, registerBtn, loginLink);
    }

    private Button createRegisterButton() {
        Button button = new Button("S'INSCRIRE");
        button.getStyleClass().add("auth-button");
        button.setOnAction(this::handleRegisterAction);
        return button;
    }

    private Hyperlink createLoginLink() {
        Hyperlink link = new Hyperlink("Déjà inscrit ? Connectez-vous");
        link.getStyleClass().add("auth-link");
        link.setOnAction(e -> app.showLoginView());
        return link;
    }

    private void setupCenterLayout(VBox registerBox) {
        StackPane centeringPane = new StackPane(registerBox);
        centeringPane.setPadding(new Insets(20));
        layout.setCenter(centeringPane);
    }

    private void handleRegisterAction(ActionEvent event) {
        try {
            controller.handleRegistration(
                    usernameField.getText().trim(),
                    emailField.getText().trim(),
                    passwordField.getText().trim(),
                    confirmPasswordField.getText().trim()
            );
            showAlert("Succès", "Compte créé avec succès !");
        } catch (IllegalArgumentException ex) {
            showAlert("Erreur", ex.getMessage());
        } catch (IllegalStateException ex) {
            showAlert("Échec de l'inscription", ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible de communiquer avec le serveur.");
        }
    }

    public Pane getView() {
        return layout;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStyleClass().add("auth-alert");
        alert.showAndWait();
    }
}