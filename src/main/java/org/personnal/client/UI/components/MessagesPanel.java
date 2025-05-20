package org.personnal.client.UI.components;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import org.personnal.client.controller.ChatController;
import org.personnal.client.model.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Composant pour la liste des messages (panneau droit)
 * Avec application des styles CSS
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
    private Button attachButton;
    private Label contactHeaderLabel;
    private ProgressIndicator sendingIndicator;
    private StackPane footerPane;

    // Action d'envoi de message
    private final Runnable onSendMessage;

    // État
    private String currentChatPartner = null;

    // Pour l'affichage du statut de saisie
    private final ScheduledExecutorService typingScheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean isTyping = false;
    private Label typingIndicator;

    // Ensemble des cellules actuellement visibles pour optimiser les mises à jour
    private final Set<ListCell<Message>> visibleCells = new HashSet<>();
    private final StringProperty currentChatPartnerProperty = new SimpleStringProperty(null);
    private final Runnable onCallButtonClicked;
    /**
     * Constructeur du panneau de messages
     */
    public MessagesPanel(ChatController controller, ObservableList<Message> messages,
                         Runnable onSendMessage, Runnable onCallButtonClicked) {
        this.panel = new BorderPane();
        this.controller = controller;
        this.messages = messages;
        this.onSendMessage = onSendMessage;
        this.onCallButtonClicked = onCallButtonClicked;

        // Appliquer le style global au panneau
        panel.getStyleClass().add("messages-panel");

        setupPanel();
    }

    /**
     * Configure le panneau des messages
     */
    private void setupPanel() {
        // En-tête de conversation
        HBox chatHeader = createChatHeader();

        // Zone de messages
        messageListView = new ListView<>(messages);
        messageListView.getStyleClass().add("message-list");
        messageListView.setCellFactory(createMessageCellFactory());
        VBox.setVgrow(messageListView, Priority.ALWAYS);

        // Zone de saisie et envoi de message
        footerPane = createMessageInputBox();

        panel.setTop(chatHeader);
        panel.setCenter(messageListView);
        panel.setBottom(footerPane);
    }

    /**
     * Crée l'en-tête de la conversation
     */
    private HBox createChatHeader() {
        HBox chatHeader = new HBox();
        chatHeader.getStyleClass().add("messages-header");

        // Label pour le nom du contact
        contactHeaderLabel = new Label("Sélectionnez un contact pour discuter");
        contactHeaderLabel.getStyleClass().add("contact-header-name");

        // Indicateur de saisie
        typingIndicator = new Label("en train d'écrire...");
        typingIndicator.getStyleClass().add("typing-indicator");
        typingIndicator.setVisible(false);

        VBox headerContent = new VBox(3);
        headerContent.getChildren().addAll(contactHeaderLabel, typingIndicator);

        // Actions supplémentaires
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton d'appel
        Button callButton = new Button("📞");
        callButton.getStyleClass().add("call-button");
        callButton.setTooltip(new Tooltip("Appeler ce contact"));
        callButton.setOnAction(e -> {
            if (currentChatPartner != null) {
                onCallButtonClicked.run();
            }
        });
        // Désactiver le bouton si aucun contact n'est sélectionné
        callButton.disableProperty().bind(Bindings.isNull(currentChatPartnerProperty));

        chatHeader.getChildren().addAll(headerContent, spacer, callButton);

        return chatHeader;
    }

    /**
     * Crée la fabrique de cellules pour la liste des messages
     */
    private Callback<ListView<Message>, ListCell<Message>> createMessageCellFactory() {
        return listView -> new ListCell<Message>() {
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

                    // Appliquer les styles CSS
                    if (isSentByMe) {
                        messageBox.getStyleClass().add("message-sent");
                        messageContainer.setAlignment(Pos.CENTER_RIGHT);
                    } else {
                        messageBox.getStyleClass().add("message-received");
                        messageContainer.setAlignment(Pos.CENTER_LEFT);

                        // Ajouter le nom de l'expéditeur uniquement si c'est un message reçu
                        // et qu'il ne suit pas un autre message du même expéditeur
                        if (!message.getSender().equals(getItem() != null && getIndex() > 0 ?
                                getListView().getItems().get(getIndex() - 1).getSender() : null)) {
                            Label senderLabel = new Label(message.getSender());
                            senderLabel.getStyleClass().add("message-sender");
                            messageBox.getChildren().add(senderLabel);
                        }
                    }

                    // Contenu du message
                    if (message.getContent().startsWith("Fichier: ")) {
                        // Affichage spécial pour les fichiers
                        String fileName = message.getContent().substring("Fichier: ".length());
                        HBox fileBox = createFileDisplay(fileName, message.getIdMessage());
                        fileBox.getStyleClass().add("file-box");
                        messageBox.getChildren().add(fileBox);
                    } else {
                        Text messageText = new Text(message.getContent());
                        messageText.getStyleClass().add("message-content");
                        messageBox.getChildren().add(messageText);
                    }

                    // Timestamp
                    Text timeText = new Text(formatTimestamp(message.getTimestamp()));
                    timeText.getStyleClass().add("message-time");

                    HBox timeBox = new HBox(timeText);
                    timeBox.setAlignment(isSentByMe ? Pos.BASELINE_RIGHT : Pos.BASELINE_LEFT);
                    messageBox.getChildren().add(timeBox);

                    messageContainer.getChildren().add(messageBox);
                    setGraphic(messageContainer);
                }
            }
        };
    }

    /**
     * Crée un affichage pour un fichier
     */
    private HBox createFileDisplay(String fileName, int fileId) {
        HBox fileBox = new HBox(5);
        fileBox.setAlignment(Pos.CENTER_LEFT);

        // Icône
        Label fileIcon = new Label("📎");
        fileIcon.getStyleClass().add("file-icon");

        // Nom du fichier
        Label fileNameLabel = new Label(fileName);
        fileNameLabel.getStyleClass().add("file-name");

        // Bouton pour télécharger/ouvrir le fichier
        Button openButton = new Button("Ouvrir");
        openButton.getStyleClass().add("file-open-button");
        openButton.setOnAction(e -> {
            // Logique pour ouvrir le fichier
            controller.openFile(fileId);
        });

        fileBox.getChildren().addAll(fileIcon, fileNameLabel, openButton);
        return fileBox;
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
    private StackPane createMessageInputBox() {
        StackPane stackPane = new StackPane();
        stackPane.getStyleClass().add("message-input-area");

        // Conteneur principal
        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER);

        // Bouton pour joindre un fichier
        attachButton = new Button("📎");
        attachButton.getStyleClass().add("attach-button");
        attachButton.setOnAction(e -> controller.handleAttachment());

        // Champ de saisie de message
        messageField = new TextField();
        messageField.setPromptText("Saisissez un message");
        messageField.getStyleClass().add("message-input-field");
        HBox.setHgrow(messageField, Priority.ALWAYS);

        // Touche "Entrée" pour envoyer
        messageField.setOnAction(e -> onSendMessage.run());

        // Détecter la saisie pour l'indicateur "en train d'écrire"
        messageField.setOnKeyPressed(e -> {
            if (e.getCode() != KeyCode.ENTER && !isTyping && currentChatPartner != null) {
                isTyping = true;

                // Réinitialiser après 2 secondes
                typingScheduler.schedule(() -> {
                    isTyping = false;
                }, 2, TimeUnit.SECONDS);
            }
        });

        // Bouton d'envoi
        sendButton = new Button("↑");
        sendButton.getStyleClass().add("send-button");
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> onSendMessage.run());

        // Activer/désactiver le bouton d'envoi selon la saisie
        messageField.textProperty().addListener((obs, old, newValue) ->
                sendButton.setDisable(newValue.trim().isEmpty() || currentChatPartner == null));

        inputBox.getChildren().addAll(attachButton, messageField, sendButton);

        // Indicateur de progression pour l'envoi
        sendingIndicator = new ProgressIndicator();
        sendingIndicator.setMaxSize(30, 30);
        sendingIndicator.setVisible(false);

        stackPane.getChildren().addAll(inputBox, sendingIndicator);
        StackPane.setAlignment(sendingIndicator, Pos.CENTER_RIGHT);
        StackPane.setMargin(sendingIndicator, new Insets(0, 50, 0, 0));

        return stackPane;
    }

    /**
     * Fait défiler la liste vers le dernier message de manière optimisée
     */
    public void scrollToLastMessage() {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;

            // Mise à jour différée pour éviter les problèmes de rendu
            Platform.runLater(() -> {
                messageListView.scrollTo(lastIndex);

                // Si le message est actuellement sélectionné mais qu'on souhaite l'afficher sans sélection
                if (messageListView.getSelectionModel().getSelectedIndex() == lastIndex) {
                    messageListView.getSelectionModel().clearSelection();
                }
            });
        }
    }


    /**
     * Définit le partenaire de chat actuel
     */
    public void setCurrentChatPartner(String partner) {
        this.currentChatPartner = partner;
        this.currentChatPartnerProperty.set(partner);

        // Activer l'input si un contact est sélectionné
        messageField.setDisable(partner == null);
        sendButton.setDisable(messageField.getText().trim().isEmpty() || partner == null);
        attachButton.setDisable(partner == null);
    }

    /**
     * Définit le texte de l'en-tête
     */
    public void setContactHeader(String text) {
        contactHeaderLabel.setText(text);
    }

    /**
     * Affiche l'indicateur "en train d'écrire"
     */
    public void showTypingIndicator(boolean show) {
        if (show != typingIndicator.isVisible()) {
            Platform.runLater(() -> typingIndicator.setVisible(show));
        }
    }

    /**
     * Montre l'indicateur de progression lors de l'envoi de message
     */
    public void showSendingIndicator(boolean show) {
        sendingIndicator.setVisible(show);
        sendButton.setDisable(show);
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

        // Donner le focus au champ de texte après l'envoi
        Platform.runLater(() -> messageField.requestFocus());
    }

    /**
     * Retourne le panneau principal
     */
    public BorderPane getPanel() {
        return panel;
    }

    /**
     * Nettoie les ressources à la fermeture
     */
    public void cleanup() {
        typingScheduler.shutdownNow();
    }
}