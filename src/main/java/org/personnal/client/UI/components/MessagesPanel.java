package org.personnal.client.UI.components;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.personnal.client.controller.ChatController;
import org.personnal.client.model.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Composant pour la liste des messages (panneau droit)
 */
public class MessagesPanel {
    // Panneau principal
    private final BorderPane panel;

    // Controller et données
    private final ChatController controller;
    private final ObservableList<Message> messages;

    // Composants UI
    private ListView<Message> messageListView;
    private TextField messageField;
    private Button sendButton;
    private Label contactHeaderLabel;

    // Action d'envoi de message
    private final Runnable onSendMessage;

    // État
    private String currentChatPartner = null;

    /**
     * Constructeur du panneau de messages
     * @param controller Contrôleur de chat
     * @param messages Liste observable des messages
     * @param onSendMessage Action à effectuer lors de l'envoi d'un message
     */
    public MessagesPanel(ChatController controller, ObservableList<Message> messages, Runnable onSendMessage) {
        this.panel = new BorderPane();
        this.controller = controller;
        this.messages = messages;
        this.onSendMessage = onSendMessage;

        setupPanel();
    }

    /**
     * Configure le panneau des messages
     */
    private void setupPanel() {
        // En-tête de conversation
        contactHeaderLabel = new Label("Sélectionnez un contact pour discuter");
        contactHeaderLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        contactHeaderLabel.setPadding(new Insets(15));
        HBox chatHeader = new HBox(contactHeaderLabel);
        chatHeader.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        // Zone de messages
        messageListView = new ListView<>(messages);
        messageListView.setCellFactory(createMessageCellFactory());
        VBox.setVgrow(messageListView, Priority.ALWAYS);
        messageListView.setStyle("-fx-background-color: #e5ddd5;");
        messageListView.setFocusTraversable(false);

        // Zone de saisie et envoi de message
        HBox messageInputBox = createMessageInputBox();

        panel.setTop(chatHeader);
        panel.setCenter(messageListView);
        panel.setBottom(messageInputBox);
    }

    /**
     * Crée la fabrique de cellules pour la liste des messages
     */
    private Callback<ListView<Message>, ListCell<Message>> createMessageCellFactory() {
        return listView -> new ListCell<>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);

                if (empty || message == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox messageContainer = new HBox(10);
                    messageContainer.setPadding(new Insets(5, 10, 5, 10));

                    // Vérifier si c'est un message envoyé ou reçu
                    boolean isSentByMe = message.getSender().equals(controller.getCurrentUsername());

                    // Créer la bulle de message
                    VBox messageBox = new VBox(5);
                    messageBox.setPadding(new Insets(10));
                    messageBox.setMaxWidth(300);

                    // Contenu du message
                    Text messageText = new Text(message.getContent());
                    messageText.setWrappingWidth(280);

                    // Timestamp
                    Text timeText = new Text(formatTimestamp(message.getTimestamp()));
                    timeText.setFont(Font.font("Arial", 10));
                    timeText.setFill(Color.GRAY);

                    HBox timeBox = new HBox(timeText);
                    timeBox.setAlignment(isSentByMe ? Pos.BASELINE_RIGHT : Pos.BASELINE_LEFT);

                    messageBox.getChildren().addAll(messageText, timeBox);

                    // Style selon le type de message
                    if (isSentByMe) {
                        messageBox.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 10; -fx-border-radius: 10;");
                        messageContainer.setAlignment(Pos.CENTER_RIGHT);
                        messageContainer.getChildren().add(messageBox);
                    } else {
                        messageBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #eaeaea; -fx-border-width: 1;");

                        // Avatar pour l'expéditeur
                        if (!message.getSender().equals(getItem() != null && getIndex() > 0 ?
                                getListView().getItems().get(getIndex() - 1).getSender() : null)) {
                            Label senderLabel = new Label(message.getSender());
                            senderLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                            senderLabel.setTextFill(Color.ROYALBLUE);
                            messageBox.getChildren().add(0, senderLabel);
                        }

                        messageContainer.setAlignment(Pos.CENTER_LEFT);
                        messageContainer.getChildren().add(messageBox);
                    }

                    setGraphic(messageContainer);
                }
            }
        };
    }

    /**
     * Formate l'horodatage pour affichage
     */
    private String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return timestamp.format(formatter);
    }

    /**
     * Crée la zone de saisie de message
     */
    private HBox createMessageInputBox() {
        HBox inputBox = new HBox(10);
        inputBox.setPadding(new Insets(10));
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setStyle("-fx-background-color: #f0f0f0;");

        // Bouton pour joindre un fichier
        Button attachButton = new Button("📎");
        attachButton.setOnAction(e -> controller.handleAttachment());

        // Champ de saisie de message
        messageField = new TextField();
        messageField.setPromptText("Saisissez un message");
        HBox.setHgrow(messageField, Priority.ALWAYS);

        // Touche "Entrée" pour envoyer
        messageField.setOnAction(e -> onSendMessage.run());

        // Bouton d'envoi
        sendButton = new Button("↑");
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> onSendMessage.run());

        // Activer/désactiver le bouton d'envoi selon la saisie
        messageField.textProperty().addListener((obs, old, newValue) ->
                sendButton.setDisable(newValue.trim().isEmpty() || currentChatPartner == null));

        inputBox.getChildren().addAll(attachButton, messageField, sendButton);
        return inputBox;
    }

    /**
     * Fait défiler la liste vers le dernier message
     */
    public void scrollToLastMessage() {
        if (!messages.isEmpty()) {
            messageListView.scrollTo(messages.size() - 1);
        }
    }

    /**
     * Définit le partenaire de chat actuel
     */
    public void setCurrentChatPartner(String partner) {
        this.currentChatPartner = partner;

        // Activer l'input si un contact est sélectionné
        messageField.setDisable(partner == null);
        sendButton.setDisable(messageField.getText().trim().isEmpty() || partner == null);
    }

    /**
     * Définit le texte de l'en-tête
     */
    public void setContactHeader(String text) {
        contactHeaderLabel.setText(text);
    }

    /**
     * Récupère le texte du message saisi
     */
    public String getMessageText() {
        return messageField.getText().trim();
    }

    /**
     * Efface le champ de saisie de message
     */
    public void clearMessageInput() {
        messageField.clear();
    }

    /**
     * Retourne le panneau principal
     */
    public BorderPane getPanel() {
        return panel;
    }
}