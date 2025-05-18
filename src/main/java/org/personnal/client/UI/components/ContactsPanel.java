package org.personnal.client.UI.components;

import javafx.application.Platform;
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
import org.personnal.client.model.User;

import java.util.List;
import java.util.function.Consumer;

/**
 * Composant pour la liste des contacts (panneau gauche)
 */
public class ContactsPanel {
    // Panneau principal
    private final BorderPane panel;

    // Controller et données
    private final ChatController controller;
    private final ObservableList<String> contacts;

    // Composants UI
    private ListView<String> contactListView;
    private TextField searchContactField;
    private Button addContactButton;
    private Button refreshContactsButton;

    // Actions et callbacks
    private final Consumer<String> onContactSelected;
    private final Runnable onAddContactClicked;
    private final Runnable onSettingsClicked;

    /**
     * Constructeur du panneau de contacts
     * @param controller Contrôleur de chat
     * @param contacts Liste observable des contacts
     * @param onContactSelected Action à effectuer lors de la sélection d'un contact
     * @param onAddContactClicked Action à effectuer lors du clic sur le bouton d'ajout de contact
     * @param onSettingsClicked Action à effectuer lors du clic sur le bouton des paramètres
     */
    public ContactsPanel(ChatController controller, ObservableList<String> contacts,
                         Consumer<String> onContactSelected, Runnable onAddContactClicked,
                         Runnable onSettingsClicked) {
        this.panel = new BorderPane();
        this.controller = controller;
        this.contacts = contacts;
        this.onContactSelected = onContactSelected;
        this.onAddContactClicked = onAddContactClicked;
        this.onSettingsClicked = onSettingsClicked;

        setupPanel();
    }

    /**
     * Configure le panneau des contacts
     */
    private void setupPanel() {
        // En-tête avec le profil utilisateur
        HBox userHeader = createUserHeader();

        // Barre de recherche et bouton d'ajout
        HBox searchBar = createSearchBar();

        // Liste des contacts
        contactListView = new ListView<>(contacts);
        contactListView.setCellFactory(createContactCellFactory());

        // Assemblage du panneau gauche
        panel.setTop(userHeader);
        BorderPane.setMargin(userHeader, new Insets(10));

        VBox centerContent = new VBox(10);
        centerContent.getChildren().addAll(searchBar, contactListView);
        centerContent.setPadding(new Insets(0, 10, 10, 10));
        panel.setCenter(centerContent);

        // Observer la sélection de contact
        contactListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        onContactSelected.accept(newValue);
                    }
                }
        );

        // Retirer la sélection après le clic
        contactListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) Platform.runLater(() -> contactListView.getSelectionModel().clearSelection());
        });
    }

    /**
     * Crée la fabrique de cellules pour la liste des contacts
     */
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

                    // Récupérer le dernier message avec ce contact
                    String lastMessage = "";
                    List<Message> contactMessages = controller.getMessageDAO().getMessagesWith(contact);
                    if (!contactMessages.isEmpty()) {
                        Message lastMsg = contactMessages.get(contactMessages.size() - 1);
                        lastMessage = lastMsg.getContent();
                        // Tronquer le message s'il est trop long
                        if (lastMessage.length() > 30) {
                            lastMessage = lastMessage.substring(0, 27) + "...";
                        }
                    }

                    Label lastMessageLabel = new Label(lastMessage);
                    lastMessageLabel.setFont(Font.font("Arial", 12));
                    lastMessageLabel.setTextFill(Color.GRAY);

                    contactInfo.getChildren().addAll(contactName, lastMessageLabel);

                    // Statut en ligne
                    Circle onlineStatus = new Circle(5);
                    onlineStatus.setFill(controller.isUserOnline(contact) ? Color.GREEN : Color.GRAY);
                    VBox.setMargin(onlineStatus, new Insets(5, 0, 0, 0));

                    // Indicateur de messages non lus
                    boolean hasUnreadMessages = false;
                    try {
                        hasUnreadMessages = controller.getMessageDAO().hasUnreadMessagesFrom(contact);
                    } catch (Exception e) {
                        // En cas d'erreur, on suppose qu'il n'y a pas de messages non lus
                        System.err.println("Erreur lors de la vérification des messages non lus : " + e.getMessage());
                    }

                    if (hasUnreadMessages) {
                        Circle unreadIndicator = new Circle(7, Color.RED);
                        Text unreadCount = new Text("!");
                        unreadCount.setFill(Color.WHITE);
                        unreadCount.setFont(Font.font("Arial", FontWeight.BOLD, 9));
                        StackPane unreadBadge = new StackPane(unreadIndicator, unreadCount);
                        contactBox.getChildren().add(unreadBadge);
                    }

                    HBox.setHgrow(contactInfo, Priority.ALWAYS);
                    contactBox.getChildren().addAll(avatarPane, contactInfo, onlineStatus);

                    setGraphic(contactBox);
                    setText(null);
                }
            }
        };
    }

    /**
     * Crée l'en-tête avec les informations de l'utilisateur
     */
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
        settingsButton.setOnAction(e -> onSettingsClicked.run());

        // Bouton de rafraîchissement des contacts
        refreshContactsButton = new Button("🔄");
        refreshContactsButton.setOnAction(e -> refreshContactList());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(avatarPane, usernameLabel, spacer, refreshContactsButton, settingsButton);
        header.setStyle("-fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        return header;
    }

    /**
     * Crée la barre de recherche avec le bouton d'ajout de contact
     */
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
        addContactButton.setOnAction(e -> onAddContactClicked.run());

        searchBar.getChildren().addAll(searchContactField, addContactButton);
        return searchBar;
    }

    /**
     * Met à jour la cellule du contact avec le dernier message reçu
     * @param contact Nom du contact
     * @param lastMessage Contenu du dernier message
     * @param currentChatPartner Contact actuellement sélectionné
     */
    public void updateContactWithLastMessage(String contact, String lastMessage, String currentChatPartner) {
        // Mettre à jour la cellule de contact avec le dernier message
        for (int i = 0; i < contactListView.getItems().size(); i++) {
            if (contactListView.getItems().get(i).equals(contact)) {
                // Mettre à jour le texte du dernier message dans la cellule
                int finalI = i;
                Platform.runLater(() -> {
                    // Forcer le rafraîchissement de la cellule spécifique
                    contactListView.refresh();

                    // Optionnel : mettre en surbrillance temporairement le contact
                    // pour indiquer un nouveau message
                    if (!contact.equals(currentChatPartner)) {
                        ListCell<String> cell = (ListCell<String>) contactListView.lookup(".list-cell:filled:selected");
                        if (cell != null && cell.getIndex() == finalI) {
                            cell.setStyle("-fx-background-color: #e6f7ff;");
                            // Rétablir le style après un certain temps
                            new java.util.Timer().schedule(
                                    new java.util.TimerTask() {
                                        @Override
                                        public void run() {
                                            Platform.runLater(() -> cell.setStyle(""));
                                        }
                                    }, 2000 // 2 secondes
                            );
                        }
                    }
                });
                break;
            }
        }

        // Remonter le contact concerné en haut de la liste si ce n'est pas la conversation active
        if (!contact.equals(currentChatPartner) && contacts.contains(contact)) {
            Platform.runLater(() -> {
                contacts.remove(contact);
                contacts.add(0, contact);
            });
        }
    }

    /**
     * Rafraîchit la liste des contacts
     */
    public void refreshContactList() {
        contactListView.refresh();
    }

    /**
     * Retourne le panneau des contacts
     */
    public BorderPane getPanel() {
        return panel;
    }
}