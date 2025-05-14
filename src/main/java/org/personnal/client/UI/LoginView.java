package org.personnal.client.UI;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.personnal.client.MainClient;
import org.personnal.client.controller.LoginController;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import java.io.IOException;
import java.util.Map;

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
        setupBackground();
        VBox loginBox = createLoginBox();
        setupCenterLayout(loginBox);
    }

    private void setupBackground() {
        layout.setStyle("-fx-background-color: linear-gradient(to right, #1a6fc7, #2e8ede);");
    }

    private VBox createLoginBox() {
        VBox loginBox = new VBox(15);
        configureLoginBox(loginBox);
        addShadowEffect(loginBox);
        addComponentsToLoginBox(loginBox);
        return loginBox;
    }

    private void configureLoginBox(VBox box) {
        box.setPadding(new Insets(30));
        box.setMaxSize(400, 400);
        box.setMinSize(350, 350);
        box.setPrefSize(400, 400);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10px;");
    }

    private void addShadowEffect(VBox box) {
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10.0);
        dropShadow.setColor(Color.color(0, 0, 0, 0.3));
        box.setEffect(dropShadow);
    }

    private void addComponentsToLoginBox(VBox box) {
        Label appTitle = createLabel("ALANYA", 28, "#1a6fc7", FontWeight.BOLD);
        Label subtitle = createLabel("Connexion", 16, "#555", FontWeight.NORMAL);

        usernameField = createTextField("Nom d'utilisateur");
        passwordField = createPasswordField("Mot de passe");
        Button loginBtn = createLoginButton();
        Hyperlink registerLink = createRegisterLink();

        box.getChildren().addAll(appTitle, subtitle, usernameField, passwordField, loginBtn, registerLink);
    }

    private Label createLabel(String text, int size, String color, FontWeight weight) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", weight, size));
        label.setTextFill(Color.web(color));
        return label;
    }

    private TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px; " +
                "-fx-border-color: #ddd; -fx-border-radius: 5px; -fx-padding: 10px;");
        return field;
    }

    private PasswordField createPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px; " +
                "-fx-border-color: #ddd; -fx-border-radius: 5px; -fx-padding: 10px;");
        return field;
    }

    private Button createLoginButton() {
        Button button = new Button("SE CONNECTER");
        button.setMaxWidth(Double.MAX_VALUE);
        applyButtonStyle(button, "#1a6fc7");

        button.setOnMouseEntered(e -> applyButtonStyle(button, "#0d5fb7"));
        button.setOnMouseExited(e -> applyButtonStyle(button, "#1a6fc7"));
        button.setOnAction(this::handleLoginAction);

        return button;
    }

    private void applyButtonStyle(Button button, String color) {
        button.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px; " +
                        "-fx-background-radius: 5px;"
        );
    }

    private Hyperlink createRegisterLink() {
        Hyperlink link = new Hyperlink("Pas encore de compte ? Inscrivez-vous");
        link.setStyle("-fx-text-fill: #1a6fc7;");
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

        // Style de l'alerte
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        dialogPane.getStyleClass().add("custom-alert");

        alert.showAndWait();
    }
}