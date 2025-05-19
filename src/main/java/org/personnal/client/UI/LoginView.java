package org.personnal.client.UI;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.personnal.client.MainClient;
import org.personnal.client.controller.LoginController;

public class LoginView {
    private final BorderPane layout;
    private final LoginController controller;
    private final MainClient app;

    // Composants UI
    private TextField usernameField;
    private PasswordField passwordField;

    public LoginView(MainClient app, LoginController controller) {
        this.app = app;
        this.controller = controller;
        this.layout = new BorderPane();
        initUI();
    }

    private void initUI() {
        // Appliquer la classe de style au layout principal
        layout.getStyleClass().add("auth-background");

        VBox loginBox = createLoginBox();
        setupCenterLayout(loginBox);
    }

    private VBox createLoginBox() {
        VBox loginBox = new VBox(15);
        loginBox.getStyleClass().add("auth-box");
        addComponentsToLoginBox(loginBox);
        return loginBox;
    }

    private void addComponentsToLoginBox(VBox box) {
        Label appTitle = new Label("ALANYA");
        appTitle.getStyleClass().add("app-title");

        Label subtitle = new Label("Connexion");
        subtitle.getStyleClass().add("app-subtitle");

        usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");
        usernameField.getStyleClass().add("auth-text-field");

        passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.getStyleClass().add("auth-text-field");

        Button loginBtn = createLoginButton();
        Hyperlink registerLink = createRegisterLink();

        box.getChildren().addAll(appTitle, subtitle, usernameField, passwordField, loginBtn, registerLink);
    }

    private Button createLoginButton() {
        Button button = new Button("SE CONNECTER");
        button.getStyleClass().add("auth-button");
        button.setOnAction(this::handleLoginAction);
        return button;
    }

    private Hyperlink createRegisterLink() {
        Hyperlink link = new Hyperlink("Pas encore de compte ? Inscrivez-vous");
        link.getStyleClass().add("auth-link");
        link.setOnAction(e -> app.showRegisterView());
        return link;
    }

    private void setupCenterLayout(VBox loginBox) {
        StackPane centeringPane = new StackPane(loginBox);
        centeringPane.setPadding(new Insets(20));
        layout.setCenter(centeringPane);
    }

    private void handleLoginAction(ActionEvent event) {
        try {
            controller.handleLogin(
                    usernameField.getText().trim(),
                    passwordField.getText().trim()
            );
        } catch (IllegalArgumentException ex) {
            showAlert("Erreur", ex.getMessage());
        } catch (IllegalStateException ex) {
            showAlert("Échec de connexion", ex.getMessage());
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