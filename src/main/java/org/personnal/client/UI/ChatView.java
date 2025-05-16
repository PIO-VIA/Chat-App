package org.personnal.client.UI;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.personnal.client.controller.ChatController;
import org.personnal.client.model.Message;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatView {
    // Éléments principales de l'interface
    private final BorderPane mainView;
    private final BorderPane leftPanel;
    private final BorderPane rightPanel;
    private final SplitPane splitPane;
    private final ChatController controller;

    // Listes observables
    private final ObservableList<String> contacts = FXCollections.observableArrayList();
    private final ObservableList<Message> messages = FXCollections.observableArrayList();

    // Éléments UI
    private ListView<String> contactListView;
    private ListView<Message> messageListView;
    private TextField messageField;
    private Button sendButton;
    private Label currentChatPartnerLabel;
    private TextField searchContactField;
    private Button addContactButton;
    private Map<String, VBox> chatHistories = new HashMap<>();
    private String currentChatPartner = null;

    // Constantes
    private static final double LEFT_PANEL_WIDTH = 300;
    private static final double RIGHT_PANEL_MIN_WIDTH = 400;

    public ChatView(ChatController controller) {
        this.controller = controller;
        this.controller.setCurrentChatPartner(null);

        // Initialiser les composants principaux
        mainView = new BorderPane();
        leftPanel = new BorderPane();
        rightPanel = new BorderPane();
        splitPane = new SplitPane();

        setupLeftPanel();
        setupRightPanel();
        setupSplitPane();

        // Configuration globale
        mainView.setCenter(splitPane);
        applyStyles();

        // Observer la sélection de contact
        contactListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        switchConversation(newValue);
                    }
                }
        );
    }

    private void setupLeftPanel() {
        // En-tête avec le profil utilisateur
        HBox userHeader = createUserHeader();

        // Barre de recherche et bouton d'ajout
        HBox searchBar = createSearchBar();

        // Liste des contacts
        contactListView = new ListView<>(contacts);
        contactListView.setCellFactory(createContactCellFactory());

        // Assemblage du panneau gauche
        leftPanel.setTop(userHeader);
        BorderPane.setMargin(userHeader, new Insets(10));

        VBox centerContent = new VBox(10);
        centerContent.getChildren().addAll(searchBar, contactListView);
        centerContent.setPadding(new Insets(0, 10, 10, 10));
        leftPanel.setCenter(centerContent);
    }

    private Callback<ListView<String>, ListCell<String>> createContactCellFactory() {
        return listView -> new ListCell<>() {
            @Override
            protected void updateItem(String contact, boolean empty) {
                super.updateItem(contact, empty);

                if (empty || contact == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox contactBox = new HBox(10);
                    contactBox.setAlignment(Pos.CENTER_LEFT);
                    contactBox.setPadding(new Insets(5, 0, 5, 0));

                    // Avatar
                    Circle avatar = new Circle(20, Color.LIGHTBLUE);
                    Text initial = new Text(contact.substring(0, 1).toUpperCase());
                    initial.setFill(Color.WHITE);
                    StackPane avatarPane = new StackPane(avatar, initial);

                    // Info contact
                    VBox contactInfo = new VBox(2);
                    Label contactName = new Label(contact);
                    contactName.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                    Label lastMessage = new Label("Cliquez pour commencer à discuter");
                    lastMessage.setFont(Font.font("Arial", 12));
                    lastMessage.setTextFill(Color.GRAY);
                    contactInfo.getChildren().addAll(contactName, lastMessage);

                    // Statut en ligne
                    Circle onlineStatus = new Circle(5);
                    onlineStatus.setFill(controller.isUserOnline(contact) ? Color.GREEN : Color.GRAY);
                    VBox.setMargin(onlineStatus, new Insets(5, 0, 0, 0));

                    HBox.setHgrow(contactInfo, Priority.ALWAYS);
                    contactBox.getChildren().addAll(avatarPane, contactInfo, onlineStatus);

                    setGraphic(contactBox);
                    setText(null);
                }
            }
        };
    }

    private HBox createUserHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));

        // Avatar de l'utilisateur
        Circle userAvatar = new Circle(20);
        userAvatar.setFill(Color.ROYALBLUE);
        Text userInitial = new Text(controller.getCurrentUsername().substring(0, 1).toUpperCase());
        userInitial.setFill(Color.WHITE);
        StackPane avatarPane = new StackPane(userAvatar, userInitial);

        // Nom d'utilisateur
        Label usernameLabel = new Label(controller.getCurrentUsername());
        usernameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));



        Button settingsButton = new Button("⚙️");
        settingsButton.setOnAction(e -> showSettingsDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(avatarPane, usernameLabel, spacer,  settingsButton);
        header.setStyle("-fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        return header;
    }

    private HBox createSearchBar() {
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER);

        // Champ de recherche
        searchContactField = new TextField();
        searchContactField.setPromptText("Rechercher ou commencer une discussion");
        HBox.setHgrow(searchContactField, Priority.ALWAYS);
        searchContactField.textProperty().addListener((observable, oldValue, newValue) -> {
            controller.filterContacts(newValue);
        });

        // Bouton d'ajout de contact
        addContactButton = new Button("+");
        addContactButton.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        addContactButton.setOnAction(e -> showAddContactDialog());

        searchBar.getChildren().addAll(searchContactField, addContactButton);
        return searchBar;
    }

    private void setupRightPanel() {
        // En-tête de conversation
        currentChatPartnerLabel = new Label("Sélectionnez un contact pour discuter");
        currentChatPartnerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        currentChatPartnerLabel.setPadding(new Insets(15));
        HBox chatHeader = new HBox(currentChatPartnerLabel);
        chatHeader.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        // Zone de messages
        messageListView = new ListView<>(messages);
        messageListView.setCellFactory(createMessageCellFactory());
        VBox.setVgrow(messageListView, Priority.ALWAYS);

        // Zone de saisie et envoi de message
        HBox messageInputBox = createMessageInputBox();

        rightPanel.setTop(chatHeader);
        rightPanel.setCenter(messageListView);
        rightPanel.setBottom(messageInputBox);
    }

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

    private String formatTimestamp(LocalDateTime timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(new Date(String.valueOf(timestamp)));
    }

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
        messageField.setOnAction(e -> sendMessage());

        // Bouton d'envoi
        sendButton = new Button("↑");
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> sendMessage());

        // Activer/désactiver le bouton d'envoi selon la saisie
        messageField.textProperty().addListener((obs, old, newValue) ->
                sendButton.setDisable(newValue.trim().isEmpty() || currentChatPartner == null));

        inputBox.getChildren().addAll(attachButton, messageField, sendButton);
        return inputBox;
    }

    private void setupSplitPane() {
        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.3);
        SplitPane.setResizableWithParent(leftPanel, false);

        leftPanel.setPrefWidth(LEFT_PANEL_WIDTH);
        rightPanel.setMinWidth(RIGHT_PANEL_MIN_WIDTH);
    }

    private void applyStyles() {
        // Appliquer les styles globaux
        mainView.setStyle("-fx-background-color: white;");
        messageListView.setStyle("-fx-background-color: #e5ddd5;");

        // Retirer la bordure de sélection des listes
        messageListView.setFocusTraversable(false);
        contactListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) Platform.runLater(() -> contactListView.getSelectionModel().clearSelection());
        });
    }

    private void switchConversation(String contact) {
        // Changer le partenaire de discussion actuel
        controller.setCurrentChatPartner(contact);
        currentChatPartner = contact;
        currentChatPartnerLabel.setText(contact);

        // Charger les messages pour ce contact
        messages.clear();
        controller.loadMessagesForContact(contact).forEach(messages::add);

        // Activer l'input si un contact est sélectionné
        messageField.setDisable(false);
        sendButton.setDisable(messageField.getText().trim().isEmpty());

        // Défilement vers le dernier message
        Platform.runLater(() -> {
            if (!messages.isEmpty()) {
                messageListView.scrollTo(messages.size() - 1);
            }
        });
    }

    private void sendMessage() {
        String content = messageField.getText().trim();
        if (!content.isEmpty() && currentChatPartner != null) {
            boolean success = controller.sendMessage(currentChatPartner, content);
            if (success) {
                messageField.clear();
                // Le message sera ajouté via la mise à jour de l'observable list
            }
        }
    }

    /**
     * Ajoute un message à la conversation actuelle
     * @param message Le message à ajouter
     */
    public void addMessageToConversation(Message message) {
        Platform.runLater(() -> {
            // Ajouter le message à la conversation correspondante
            String partner = message.getSender().equals(controller.getCurrentUsername())
                    ? message.getReceiver() : message.getSender();

            // Si c'est la conversation active, mettre à jour la vue
            if (partner.equals(currentChatPartner)) {
                messages.add(message);
                Platform.runLater(() -> messageListView.scrollTo(messages.size() - 1));
            }

            // Mettre à jour la cellule du contact dans la liste pour montrer le dernier message
            updateContactWithLastMessage(partner, message.getContent());
        });
    }

    private void updateContactWithLastMessage(String contact, String lastMessage) {
        // Mettre à jour la cellule de contact avec le dernier message
        // (Cette mise à jour sera visible lors du prochain rafraîchissement de la cellule)
        contactListView.refresh();
    }

    /**
     * Définit la liste des contacts disponibles
     * @param contactsList Liste des contacts
     */
    public void setContacts(ObservableList<String> contactsList) {
        Platform.runLater(() -> {
            contacts.clear();
            contacts.addAll(contactsList);
        });
    }

    /**
     * Ajoute un nouveau contact à la liste
     * @param contact Nom du contact à ajouter
     */
    public void addContact(String contact) {
        if (!contacts.contains(contact)) {
            Platform.runLater(() -> contacts.add(contact));
        }
    }

    /**
     * Supprime un contact de la liste
     * @param contact Nom du contact à supprimer
     */
    public void removeContact(String contact) {
        Platform.runLater(() -> contacts.remove(contact));
    }

    /**
     * Dialogue pour ajouter un nouveau contact
     */
    private void showAddContactDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ajouter un contact");
        dialog.setHeaderText("Entrez le nom d'utilisateur du contact à ajouter");
        dialog.setContentText("Nom d'utilisateur:");

        dialog.showAndWait().ifPresent(username -> {
            if (!username.trim().isEmpty()) {
                controller.addContact(username);
            }
        });
    }

    /**
     * Dialogue des paramètres
     */
    private void showSettingsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Paramètres");
        alert.setHeaderText("Paramètres utilisateur");
        alert.setContentText("Cette fonctionnalité sera disponible prochainement.");
        alert.showAndWait();
    }

    /**
     * Retourne la vue principale
     * @return BorderPane contenant toute l'interface
     */
    public BorderPane getView() {
        return mainView;
    }

    /**
     * Met à jour le statut en ligne d'un contact
     * @param contact Nom du contact
     * @param online Statut en ligne
     */
    public void updateContactStatus(String contact, boolean online) {
        Platform.runLater(() -> contactListView.refresh());
    }
}