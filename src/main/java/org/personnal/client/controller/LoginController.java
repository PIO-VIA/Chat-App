package org.personnal.client.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class LoginController extends VBox {
    private int spacing;
    public LoginController(int spacing){
        super(spacing);
        this.setPadding(new Insets(30));
        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-background-color: #f9f9f9;");

        Label title = new Label("🔐 Connexion");
        title.setFont(Font.font(22));
        title.setTextFill(Color.web("#333"));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");

        Button loginBtn = new Button("Se connecter");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        Label switchToRegister = new Label("Pas encore de compte ? Cliquez ici");
        switchToRegister.setStyle("-fx-text-fill: #0077cc; -fx-underline: true;");
        this.getChildren().addAll(title, usernameField, passwordField, loginBtn, switchToRegister);

    }
}
