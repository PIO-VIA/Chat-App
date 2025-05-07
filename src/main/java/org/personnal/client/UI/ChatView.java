package org.personnal.client.UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.personnal.client.controller.ChatController;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ChatView {

    private final BorderPane layout;
    private final ChatController controller;
    // Couleurs de l'application (style WhatsApp mais bleu au lieu de vert)
    private final String COLOR_PRIMARY = "#1E88E5";       // Bleu primaire (au lieu du vert #075E54)
    private final String COLOR_SECONDARY = "#2196F3";     // Bleu clair (au lieu du vert #25D366)
    private final String COLOR_BACKGROUND = "#ECE5DD";    // Fond gris clair comme WhatsApp
    private final String COLOR_CHAT_BG = "#E4E4E4";       // Fond des messages
    private final String COLOR_MESSAGE_OUT = "#DCF8F8";   // Bulles de message envoyé (bleuté)
    private final String COLOR_MESSAGE_IN = "#FFFFFF";    // Bulles de message reçu (blanc)

    public ChatView(ChatController controller) {
        this.controller = controller;
        this.layout = new BorderPane();
        initUI();
    }

    private void initUI() {
        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: " + COLOR_PRIMARY + ";");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        // Avatar (cercle avec les initiales)
        StackPane avatar = createAvatar(controller.getCurrentUsername());

        VBox userInfo = new VBox(2);
        Label usernameLabel = new Label(controller.getCurrentUsername());
        usernameLabel.setTextFill(Color.WHITE);
        usernameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label statusLabel = new Label("En ligne");
        statusLabel.setTextFill(Color.rgb(255, 255, 255, 0.8));
        statusLabel.setFont(Font.font("System", 12));

        userInfo.getChildren().addAll(usernameLabel, statusLabel);

        // Boutons header
        HBox headerButtons = new HBox(15);
        headerButtons.setAlignment(Pos.CENTER_RIGHT);

        Button searchButton = createIconButton("🔍", "Rechercher");
        Button menuButton = createIconButton("⋮", "Menu");

        headerButtons.getChildren().addAll(searchButton, menuButton);
        HBox.setHgrow(headerButtons, Priority.ALWAYS);

        header.getChildren().addAll(avatar, userInfo, headerButtons);

        // Liste utilisateurs
        ListView<String> userList = controller.getUserListView();
        userList.setPrefWidth(280);
        userList.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #E1E1E1;" +
                "-fx-border-width: 0 1 0 0;");

        // Button pour actualiser la liste des utilisateurs
        Button refreshButton = createIconButton("🔄", "Actualiser la liste");
        refreshButton.setOnAction(e -> controller.requestConnectedUsers());

        // Personnalisation des cellules de la liste utilisateurs
        userList.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cellContent = new HBox(10);
                    cellContent.setPadding(new Insets(8));
                    cellContent.setAlignment(Pos.CENTER_LEFT);

                    // Avatar de l'utilisateur
                    StackPane userAvatar = createAvatar(item);

                    VBox textContent = new VBox(3);
                    Label nameLabel = new Label(item);
                    nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

                    // Statut en ligne (point vert)
                    Label statusLabel = new Label("●  En ligne");
                    statusLabel.setFont(Font.font("System", 12));
                    statusLabel.setTextFill(Color.web("#2196F3"));

                    textContent.getChildren().addAll(nameLabel, statusLabel);

                    // Créer un indicateur de sélection
                    StackPane indicator = new StackPane();
                    indicator.setMinWidth(4);
                    indicator.setPrefWidth(4);
                    indicator.setMaxWidth(4);
                    indicator.setMinHeight(40);
                    indicator.setStyle("-fx-background-color: transparent;");

                    // Combinaison de tous les éléments
                    cellContent.getChildren().addAll(indicator, userAvatar, textContent);
                    HBox.setMargin(indicator, new Insets(0, 0, 0, -8));

                    setGraphic(cellContent);
                    setText(null);

                    // Style pour la cellule sélectionnée
                    selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                        if (isNowSelected) {
                            indicator.setStyle("-fx-background-color: " + COLOR_PRIMARY + ";");
                            setStyle("-fx-background-color: #E8EAF6;");
                        } else {
                            indicator.setStyle("-fx-background-color: transparent;");
                            setStyle("");
                        }
                    });
                }
            }
        });

        // Barre de recherche pour les contacts
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher ou démarrer une nouvelle discussion");
        searchField.setPadding(new Insets(8));
        searchField.setStyle("-fx-background-color: #F0F0F0;" +
                "-fx-background-radius: 20;" +
                "-fx-border-radius: 20;");

        // Titre avec le nombre d'utilisateurs connectés
        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(10, 10, 10, 15));
        titleBox.setSpacing(10);

        Label userListTitle = new Label("Utilisateurs connectés");
        userListTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

        // Ajout du bouton de rafraîchissement
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        titleBox.getChildren().addAll(userListTitle, spacer, refreshButton);

        // Label du nombre d'utilisateurs (sera mis à jour dynamiquement)
        Label userCountLabel = new Label();
        userCountLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12; -fx-padding: 0 0 0 15;");

        // Ajouter un listener pour mettre à jour le compteur
        userList.getItems().addListener((javafx.collections.ListChangeListener.Change<? extends String> c) -> {
            int count = userList.getItems().size();
            userCountLabel.setText(count + (count == 1 ? " utilisateur connecté" : " utilisateurs connectés"));
        });

        VBox userListBox = new VBox(titleBox, userCountLabel, searchField, userList);
        userListBox.setSpacing(5);
        userListBox.setPadding(new Insets(0, 0, 0, 0));
        userListBox.setStyle("-fx-background-color: white;");
        VBox.setMargin(searchField, new Insets(5, 10, 10, 10));

        // Zone messages
        VBox messageArea = controller.getMessageArea();
        messageArea.setPadding(new Insets(15));
        messageArea.setStyle("-fx-background-color: " + COLOR_BACKGROUND + ";" +
                "-fx-background-image: url('file:chat-background.png');" +
                "-fx-background-size: 30%;");

        ScrollPane scrollPane = new ScrollPane(messageArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: " + COLOR_BACKGROUND + ";" +
                "-fx-border-color: #E1E1E1;" +
                "-fx-border-width: 1 0 0 0;");
        scrollPane.vvalueProperty().bind(messageArea.heightProperty());

        // Zone d'entrée
        HBox inputArea = new HBox(10);
        inputArea.setPadding(new Insets(10));
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setStyle("-fx-background-color: #F0F0F0;");

        // Bouton emoji
        Button emojiButton = createIconButton("😊", "Emoji");

        // Bouton pièce jointe
        Button attachFileButton = createIconButton("📎", "Pièce jointe");
        attachFileButton.setOnAction(e -> controller.handleFileUpload());

        // Champ de texte
        TextField messageField = controller.getMessageInputField();
        messageField.setPromptText("Écrire un message...");
        messageField.setPrefHeight(40);
        messageField.setStyle("-fx-background-radius: 20;" +
                "-fx-border-radius: 20;" +
                "-fx-padding: 5 15 5 15;");
        HBox.setHgrow(messageField, Priority.ALWAYS);

        // Bouton envoyer ou micro
        Button sendButton = controller.getSendButton();
        sendButton.setText("📤");
        sendButton.setFont(Font.font("System", 16));
        sendButton.setStyle("-fx-background-color: " + COLOR_SECONDARY + ";" +
                "-fx-background-radius: 20;" +
                "-fx-text-fill: white;" +
                "-fx-min-width: 40;" +
                "-fx-min-height: 40;" +
                "-fx-max-width: 40;" +
                "-fx-max-height: 40;");

        inputArea.getChildren().addAll(emojiButton, attachFileButton, messageField, sendButton);

        // Layout principal
        layout.setTop(header);
        layout.setLeft(userListBox);
        layout.setCenter(scrollPane);
        layout.setBottom(inputArea);
    }

    // Méthode utilitaire pour créer un avatar avec initiales
    private StackPane createAvatar(String username) {
        StackPane avatar = new StackPane();
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);
        avatar.setStyle("-fx-background-color: " + COLOR_SECONDARY + ";" +
                "-fx-background-radius: 20;");

        String initials = username.substring(0, 1).toUpperCase();
        Label initialsLabel = new Label(initials);
        initialsLabel.setTextFill(Color.WHITE);
        initialsLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        avatar.getChildren().add(initialsLabel);
        return avatar;
    }

    // Méthode utilitaire pour créer un bouton avec icône
    private Button createIconButton(String icon, String tooltip) {
        Button button = new Button(icon);
        button.setTooltip(new Tooltip(tooltip));
        button.setFont(Font.font("System", 16));
        button.setStyle("-fx-background-color: transparent;");
        button.setMinSize(30, 30);
        return button;
    }

    public BorderPane getView() {
        return layout;
    }
}