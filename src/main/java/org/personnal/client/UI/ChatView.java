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
import javafx.util.Pair;
import org.personnal.client.controller.ChatController;
import org.personnal.client.model.FileData;
import org.personnal.client.model.Message;
import org.personnal.client.model.User;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    private Button refreshContactsButton;
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

        // Charger les contacts depuis la BD locale
        updateContactList();

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

                    // Récupérer l'email du contact depuis la BD locale
                    User user = controller.getUserByUsername(contact);
                    String emailText = (user != null && user.getEmail() != null) ? user.getEmail() : "";
                    Label emailLabel = new Label(emailText);
                    emailLabel.setFont(Font.font("Arial", 12));
                    emailLabel.setTextFill(Color.GRAY);

                    contactInfo.getChildren().addAll(contactName, emailLabel);

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

        // Bouton de paramètres
        Button settingsButton = new Button("⚙️");
        settingsButton.setOnAction(e -> showSettingsDialog());

        // Bouton de rafraîchissement des contacts
        refreshContactsButton = new Button("🔄");
        refreshContactsButton.setOnAction(e -> updateContactList());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(avatarPane, usernameLabel, spacer, refreshContactsButton, settingsButton);
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
            // La liste des contacts sera mise à jour par le controller
            contacts.clear();
            contacts.addAll(controller.getContacts());
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
        if (timestamp == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return timestamp.format(formatter);
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

        // Charger les messages pour ce contact depuis la BD locale
        messages.clear();
        controller.loadMessagesForContact(contact).forEach(messages::add);

        // Activer l'input si un contact est sélectionné
        messageField.setDisable(false);
        sendButton.setDisable(messageField.getText().trim().isEmpty());

        // Afficher l'email du contact dans l'en-tête
        User user = controller.getUserByUsername(contact);
        if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
            currentChatPartnerLabel.setText(contact + " (" + user.getEmail() + ")");
        }

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

                // Recharger la conversation pour afficher le nouveau message
                messages.clear();
                controller.loadMessagesForContact(currentChatPartner).forEach(messages::add);

                // Défilement vers le dernier message
                Platform.runLater(() -> {
                    if (!messages.isEmpty()) {
                        messageListView.scrollTo(messages.size() - 1);
                    }
                });
            }
        }
    }

    /**
     * Ajoute un message à la conversation actuelle
     * @param message Le message à ajouter
     */
    public void addMessageToConversation(Message message) {
        Platform.runLater(() -> {
            // Ajouter le message à la base de données locale si c'est un message reçu
            if (!message.getSender().equals(controller.getCurrentUsername())) {
                controller.saveReceivedMessage(message);
            }

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

    /**
     * Ajoute un fichier à la conversation actuelle
     * @param file Le fichier à ajouter
     */
    public void addFileToConversation(FileData file) {
        Platform.runLater(() -> {
            // Ajouter le fichier à la base de données locale si c'est un fichier reçu
            if (!file.getSender().equals(controller.getCurrentUsername())) {
                controller.saveReceivedFile(file);
            }

            // Mettre à jour la conversation si c'est le contact actif
            String partner = file.getSender().equals(controller.getCurrentUsername())
                    ? file.getReceiver() : file.getSender();

            if (partner.equals(currentChatPartner)) {
                // Recharger tous les messages et fichiers pour ce contact
                messages.clear();
                controller.loadMessagesForContact(currentChatPartner).forEach(messages::add);

                Platform.runLater(() -> {
                    if (!messages.isEmpty()) {
                        messageListView.scrollTo(messages.size() - 1);
                    }
                });
            }

            // Mettre à jour la cellule du contact dans la liste
            updateContactWithLastMessage(partner, "Fichier: " + file.getFilename());
        });
    }

    private void updateContactWithLastMessage(String contact, String lastMessage) {
        // Mettre à jour la cellule de contact avec le dernier message
        contactListView.refresh();
    }

    /**
     * Met à jour la liste des contacts depuis la BD locale
     */
    private void updateContactList() {
        Platform.runLater(() -> {
            contacts.clear();
            contacts.addAll(controller.getContacts());
            contactListView.refresh();
        });
    }

    /**
     * Ajoute un nouveau contact à la liste
     * @param contact Nom du contact à ajouter
     */
    public void addContact(String contact) {
        if (!contacts.contains(contact)) {
            Platform.runLater(() -> {
                contacts.add(contact);
                contactListView.refresh();
            });
        }
    }

    /**
     * Supprime un contact de la liste
     * @param contact Nom du contact à supprimer
     */
    public void removeContact(String contact) {
        Platform.runLater(() -> {
            contacts.remove(contact);
            contactListView.refresh();
        });
    }

    /**
     * Dialogue pour ajouter un nouveau contact
     */
    private void showAddContactDialog() {
        // Créer un dialogue personnalisé
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un contact");
        dialog.setHeaderText("Entrez les informations du contact à ajouter");

        // Définir les boutons
        ButtonType addButtonType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Créer les champs du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        grid.add(new Label("Nom d'utilisateur:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Donner le focus au champ username
        Platform.runLater(() -> usernameField.requestFocus());

        // Convertir le résultat du dialogue
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new Pair<>(usernameField.getText(), emailField.getText());
            }
            return null;
        });

        // Afficher le dialogue et traiter le résultat
        dialog.showAndWait().ifPresent(usernameEmail -> {
            String username = usernameEmail.getKey().trim();
            String email = usernameEmail.getValue().trim();

            if (!username.isEmpty()) {
                boolean added = controller.addContact(username, email);
                if (added) {
                    // Rafraîchir la liste des contacts depuis la BD locale
                    updateContactList();

                    // Afficher un message de confirmation
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Contact ajouté");
                    alert.setHeaderText(null);
                    alert.setContentText("Le contact " + username + " a été ajouté avec succès à votre liste de contacts.");
                    alert.showAndWait();
                } else {
                    // Afficher un message d'erreur
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText(null);
                    alert.setContentText("Impossible d'ajouter le contact " + username + ".\nVérifiez que l'utilisateur existe et qu'il n'est pas déjà dans vos contacts.");
                    alert.showAndWait();
                }
            }
        });
    }

    /**
     * Dialogue des paramètres
     */
    private void showSettingsDialog() {
        // Créer un dialogue pour les paramètres
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Paramètres");
        dialog.setHeaderText("Paramètres utilisateur");

        // Ajouter des onglets pour différentes sections
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Onglet Profil
        Tab profileTab = new Tab("Profil");
        GridPane profileGrid = new GridPane();
        profileGrid.setHgap(10);
        profileGrid.setVgap(10);
        profileGrid.setPadding(new Insets(20, 150, 10, 10));

        // Informations sur l'utilisateur actuel
        Label usernameLabel = new Label("Nom d'utilisateur: " + controller.getCurrentUsername());
        Label statusLabel = new Label("Statut: En ligne");

        profileGrid.add(usernameLabel, 0, 0);
        profileGrid.add(statusLabel, 0, 1);

        // Bouton de déconnexion
        Button logoutButton = new Button("Déconnexion");
        logoutButton.setOnAction(e -> {
            controller.disconnect();
            dialog.close();
            // Rediriger vers l'écran de connexion (à implémenter)
        });

        profileGrid.add(logoutButton, 0, 3);
        profileTab.setContent(profileGrid);

        // Onglet Contacts
        Tab contactsTab = new Tab("Contacts");
        VBox contactsBox = new VBox(10);
        contactsBox.setPadding(new Insets(10));

        // Liste des contacts pour gestion (suppression, blocage, etc.)
        ListView<User> contactsListView = new ListView<>();
        contactsListView.setItems(controller.getUsersList());
        contactsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);

                    // Avatar
                    Circle avatar = new Circle(15, Color.LIGHTBLUE);
                    Text initial = new Text(user.getUsername().substring(0, 1).toUpperCase());
                    initial.setFill(Color.WHITE);
                    StackPane avatarPane = new StackPane(avatar, initial);

                    // Infos utilisateur
                    VBox userInfo = new VBox(2);
                    Label username = new Label(user.getUsername());
                    username.setFont(Font.font("Arial", FontWeight.BOLD, 13));
                    Label email = new Label(user.getEmail() != null ? user.getEmail() : "");
                    email.setFont(Font.font("Arial", 11));
                    email.setTextFill(Color.GRAY);

                    userInfo.getChildren().addAll(username, email);
                    box.getChildren().addAll(avatarPane, userInfo);

                    setGraphic(box);
                }
            }
        });

        Button deleteContactButton = new Button("Supprimer le contact sélectionné");
        deleteContactButton.setDisable(true);

        contactsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newValue) ->
                deleteContactButton.setDisable(newValue == null));

        deleteContactButton.setOnAction(e -> {
            User selectedUser = contactsListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirmation");
                confirmAlert.setHeaderText("Supprimer un contact");
                confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer " + selectedUser.getUsername() + " de vos contacts?");

                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // Supprimer le contact de la BD locale
                    if (controller.deleteContact(selectedUser.getIdUser())) {
                        // Mettre à jour la liste des contacts
                        contactsListView.setItems(controller.getUsersList());
                        updateContactList();

                        // Afficher un message de confirmation
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Contact supprimé");
                        successAlert.setHeaderText(null);
                        successAlert.setContentText("Le contact a été supprimé avec succès.");
                        successAlert.showAndWait();
                    }
                }
            }
        });

        contactsBox.getChildren().addAll(
                new Label("Gérer vos contacts:"),
                contactsListView,
                deleteContactButton
        );

        contactsTab.setContent(contactsBox);

        // Ajouter les onglets au TabPane
        tabPane.getTabs().addAll(profileTab, contactsTab);

        // Ajouter les boutons au dialogue
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(tabPane);

        // Afficher le dialogue
        dialog.showAndWait();
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