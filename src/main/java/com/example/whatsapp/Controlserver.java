package com.example.whatsapp;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;



public class Controlserver  extends BorderPane {

    public Controlserver() {

        this.setStyle("-fx-background-color: #f0f0f0;");

        // -----------------------------------------------
        // Partie GAUCHE (Liste des conversations)
        // -----------------------------------------------
        VBox leftPanel = new VBox();
        leftPanel.setPrefWidth(300);
        leftPanel.setStyle("-fx-background-color: white;");

        // Barre de recherche
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher ou démarrer une discussion");
        HBox searchBox = new HBox(searchField);
        searchBox.setPadding(new Insets(20));

        // Liste des conversations


        // Ajout de conversations fictives


        // Créer une ListView pour afficher les contacts
        ListView<Contact> contactListView = new ListView<>();
        contactListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Contact> call(ListView<Contact> param) {
                return new ContactListCell();
            }
        });

        // Ajouter des contacts fictifs
        contactListView.getItems().addAll(
                new Contact("Alice", "En ligne"),
                new Contact("Bob", " 2 heures"),
                new Contact("Charlie", "En ligne"),
                new Contact("Alice", "En ligne"),
                new Contact("Bob", " 2 heures"),
                new Contact("Alice", "En ligne"),
                new Contact("Bob", " 2 heures"),
                new Contact("Alice", "En ligne"),
                new Contact("Bob", " 2 heures")
        );

        // Gérer le clic sur un contact
        contactListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {

                Contact selectedContact = contactListView.getSelectionModel().getSelectedItem();
                if (selectedContact != null) {
                    System.out.println("Contact sélectionné : " + selectedContact.getName());
                }
            }
        });
        //chatList.getChildren().addAll(contactListView);
        /*for (int i = 1; i <= 10; i++) {
            chatList.getChildren().add(createChatItem("Contact " + i, "Dernier message...", "10:30"));
        }*/

        leftPanel.getChildren().addAll(searchBox, contactListView);

        // -----------------------------------------------
        // Partie CENTRALE (Conversation active)
        // -----------------------------------------------
        BorderPane centerPanel = new BorderPane();
        centerPanel.setStyle("-fx-background-color: #efeae2;");

        // En-tête de conversation
        HBox header = new HBox(10);
        header.setStyle("-fx-background-color: #f0f2f5; -fx-padding: 10;");
        header.getChildren().addAll(
                new Label("Contact 1"),
                new Button("Appel"),
                new Button("Infos")
        );

        // Zone de messages
        ScrollPane messageScroll = new ScrollPane();
        VBox messageContainer = new VBox(10);
        messageContainer.setPadding(new Insets(20));

        // Messages fictifs
        messageContainer.getChildren().addAll(
                createMessageBubble("Salut !", true),
                createMessageBubble("Comment ça va ?", false),
                createMessageBubble("Tout va bien, merci !", true)
        );

        messageScroll.setContent(messageContainer);
        messageScroll.setFitToWidth(true);

        // Zone de saisie
        HBox inputBox = new HBox(10);
        inputBox.setPadding(new Insets(10));
        TextField messageField = new TextField();
        Button sendButton = new Button("Envoyer");
        Button sendFile = new Button("fichier");

        sendButton.setOnAction(e -> {
            if (!messageField.getText().isEmpty()) {
                messageContainer.getChildren().add(createMessageBubble(messageField.getText(), true));
                messageField.clear();
            }
        });

        inputBox.getChildren().addAll(messageField, sendButton, sendFile);

        // Assemblage du centre
        centerPanel.setTop(header);
        centerPanel.setCenter(messageScroll);
        centerPanel.setBottom(inputBox);

        // -----------------------------------------------
        // Configuration finale
        // -----------------------------------------------
        this.setLeft(leftPanel);
        this.setCenter(centerPanel);
    }
    // Crée une bulle de message
    private HBox createMessageBubble(String text, boolean isUser) {
        Label messageLabel = new Label(text);
        messageLabel.setPadding(new Insets(10));
        messageLabel.setMaxWidth(400);

        String style = isUser ?
                "-fx-background-color: #d9fdd3; -fx-background-radius: 15;" :
                "-fx-background-color: white; -fx-background-radius: 15;";

        messageLabel.setStyle(style);

        HBox container = new HBox();
        container.setPadding(new Insets(5));
        container.getChildren().add(messageLabel);

        if (isUser) container.setAlignment(Pos.CENTER_RIGHT);
        else container.setAlignment(Pos.CENTER_LEFT);

        return container;
    }

    // Crée un élément de liste de conversation
    private HBox createChatItem(String name, String lastMessage, String time) {
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label messageLabel = new Label(lastMessage);
        messageLabel.setTextFill(Color.GRAY);

        VBox textContainer = new VBox(3, nameLabel, messageLabel);
        textContainer.setPadding(new Insets(5));

        Label timeLabel = new Label(time);
        timeLabel.setTextFill(Color.GRAY);

        HBox chatItem = new HBox(10, new CircleAvatar(), textContainer, timeLabel);
        chatItem.setPadding(new Insets(5));
        chatItem.setAlignment(Pos.CENTER_LEFT);

        return chatItem;
    }
}
