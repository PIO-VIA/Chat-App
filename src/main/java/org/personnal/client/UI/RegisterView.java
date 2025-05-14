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
        setupBackground();
        VBox registerBox = createRegisterBox();
        setupCenterLayout(registerBox);
    }

    private void setupBackground() {
        layout.setStyle("-fx-background-color: linear-gradient(to right, #1a6fc7, #2e8ede);");
    }

    private VBox createRegisterBox() {
        VBox registerBox = new VBox(15);
        configureRegisterBox(registerBox);
        addShadowEffect(registerBox);
        addComponentsToRegisterBox(registerBox);
        return registerBox;
    }

    private void configureRegisterBox(VBox box) {
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

    private void addComponentsToRegisterBox(VBox box) {
        Label appTitle = createLabel("ALANYA", 28, "#1a6fc7", FontWeight.BOLD);
        Label subtitle = createLabel("Créer un compte", 16, "#555", FontWeight.NORMAL);

        usernameField = createTextField("Nom d'utilisateur");
        emailField = createTextField("example@gmail.com");
        passwordField = createPasswordField("Mot de passe");
        confirmPasswordField = createPasswordField("Confirmer le mot de passe");
        Button registerBtn = createRegisterButton();
        Hyperlink loginLink = createLoginLink();

        box.getChildren().addAll(appTitle, subtitle, usernameField, emailField,
                passwordField, confirmPasswordField, registerBtn, loginLink);
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
        applyFieldStyle(field);
        return field;
    }

    private PasswordField createPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        applyFieldStyle(field);
        return field;
    }

    private void applyFieldStyle(Control field) {
        field.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px; " +
                "-fx-border-color: #ddd; -fx-border-radius: 5px; -fx-padding: 10px;");
    }

    private Button createRegisterButton() {
        Button button = new Button("S'INSCRIRE");
        button.setMaxWidth(Double.MAX_VALUE);
        applyButtonStyle(button, "#1a6fc7");

        button.setOnMouseEntered(e -> applyButtonStyle(button, "#0d5fb7"));
        button.setOnMouseExited(e -> applyButtonStyle(button, "#1a6fc7"));
        button.setOnAction(this::handleRegisterAction);

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

    private Hyperlink createLoginLink() {
        Hyperlink link = new Hyperlink("Déjà inscrit ? Connectez-vous");
        link.setStyle("-fx-text-fill: #1a6fc7;");
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

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        dialogPane.getStyleClass().add("custom-alert");

        alert.showAndWait();
    }
}