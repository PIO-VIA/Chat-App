package org.personnal.client.UI.components;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.personnal.client.controller.ChatController;
import org.personnal.client.model.Message;
import org.personnal.client.model.User;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Composant pour la liste des contacts (panneau gauche)
 * Avec application des styles CSS
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
    private ProgressIndicator refreshProgress;

    // Actions et callbacks
    private final Consumer<String> onContactSelected;
    private final Runnable onAddContactClicked;
    private final Runnable onSettingsClicked;

    /**
     * Constructeur du panneau de contacts
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

        // Appliquer le style global au panneau
        panel.getStyleClass().add("contacts-panel");

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
        contactListView.getStyleClass().add("contact-list");
        contactListView.setCellFactory(createContactCellFactory());

        // Assemblage du panneau gauche
        panel.setTop(userHeader);
        BorderPane.setMargin(userHeader, new Insets(0));

        VBox centerContent = new VBox(0);
        centerContent.getChildren().addAll(searchBar, contactListView);
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
        return listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String contact, boolean empty) {
                super.updateItem(contact, empty);

                if (empty || contact == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Appliquer le style CSS à la cellule
                    getStyleClass().add("contact-cell");

                    HBox contactBox = new HBox(10);
                    contactBox.setAlignment(Pos.CENTER_LEFT);

                    // Avatar
                    Circle avatar = new Circle(20);
                    avatar.getStyleClass().add("contact-avatar");

                    Text initial = new Text(contact.substring(0, 1).toUpperCase());
                    initial.setFill(Color.WHITE);
                    StackPane avatarPane = new StackPane(avatar, initial);

                    // Info contact
                    VBox contactInfo = new VBox(2);

                    Label contactName = new Label(contact);
                    contactName.getStyleClass().add("contact-name");

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
                    lastMessageLabel.getStyleClass().add("contact-message");

                    contactInfo.getChildren().addAll(contactName, lastMessageLabel);

                    // Statut en ligne
                    Circle onlineStatus = new Circle(5);
                    if (controller.isUserOnline(contact)) {
                        onlineStatus.getStyleClass().add("online-status");
                    } else {
                        onlineStatus.getStyleClass().add("offline-status");
                    }

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
                        Circle unreadIndicator = new Circle(7);
                        unreadIndicator.getStyleClass().add("unread-badge");

                        Text unreadCount = new Text("!");
                        unreadCount.setFill(Color.WHITE);
                        StackPane unreadBadge = new StackPane(unreadIndicator, unreadCount);
                        contactBox.getChildren().add(unreadBadge);
                    }

                    HBox.setHgrow(contactInfo, Priority.ALWAYS);
                    contactBox.getChildren().addAll(avatarPane, contactInfo, onlineStatus);

                    setGraphic(contactBox);
                }
            }
        };
    }

    /**
     * Crée l'en-tête avec les informations de l'utilisateur
     */
    private HBox createUserHeader() {
        HBox header = new HBox(10);
        header.getStyleClass().add("contacts-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar de l'utilisateur
        Circle userAvatar = new Circle(20);
        userAvatar.getStyleClass().add("user-avatar");

        Text userInitial = new Text(controller.getCurrentUsername().substring(0, 1).toUpperCase());
        userInitial.setFill(Color.WHITE);
        StackPane avatarPane = new StackPane(userAvatar, userInitial);

        // Nom d'utilisateur
        Label usernameLabel = new Label(controller.getCurrentUsername());
        usernameLabel.getStyleClass().add("username-label");

        // Bouton de paramètres
        Button settingsButton = new Button("⚙️");
        settingsButton.getStyleClass().add("icon-button");
        settingsButton.setOnAction(e -> onSettingsClicked.run());

        // Bouton de rafraîchissement des contacts avec indicateur de progression
        StackPane refreshStack = new StackPane();
        refreshContactsButton = new Button("🔄");
        refreshContactsButton.getStyleClass().add("icon-button");
        refreshContactsButton.setTooltip(new Tooltip("Rafraîchir le statut des contacts"));
        refreshContactsButton.setOnAction(e -> refreshContactStatuses());

        refreshProgress = new ProgressIndicator();
        refreshProgress.setMaxSize(20, 20);
        refreshProgress.setVisible(false);

        refreshStack.getChildren().addAll(refreshContactsButton, refreshProgress);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(avatarPane, usernameLabel, spacer, refreshStack, settingsButton);

        return header;
    }

    /**
     * Rafraîchit le statut en ligne de tous les contacts
     */
    private void refreshContactStatuses() {
        // Désactiver le bouton et afficher l'indicateur
        refreshContactsButton.setDisable(true);
        refreshProgress.setVisible(true);

        // Lancer le rafraîchissement en arrière-plan
        new Thread(() -> {
            try {
                // Récupérer le statut de tous les contacts
                Map<String, Boolean> statuses = controller.refreshContactStatuses();

                // Mettre à jour l'interface
                Platform.runLater(() -> {
                    // Rafraîchir la liste
                    contactListView.refresh();

                    // Réactiver le bouton et masquer l'indicateur
                    refreshContactsButton.setDisable(false);
                    refreshProgress.setVisible(false);
                });
            } catch (Exception e) {
                System.err.println("Erreur lors du rafraîchissement des statuts: " + e.getMessage());

                // Réactiver le bouton et masquer l'indicateur en cas d'erreur
                Platform.runLater(() -> {
                    refreshContactsButton.setDisable(false);
                    refreshProgress.setVisible(false);
                });
            }
        }).start();
    }

    /**
     * Crée la barre de recherche avec le bouton d'ajout de contact
     */
    private HBox createSearchBar() {
        HBox searchBar = new HBox(10);
        searchBar.getStyleClass().add("search-bar");
        searchBar.setAlignment(Pos.CENTER);

        // Champ de recherche
        searchContactField = new TextField();
        searchContactField.setPromptText("Rechercher ou commencer une discussion");
        searchContactField.getStyleClass().add("search-field");
        HBox.setHgrow(searchContactField, Priority.ALWAYS);

        searchContactField.textProperty().addListener((observable, oldValue, newValue) -> {
            controller.filterContacts(newValue);
            // La liste des contacts sera mise à jour par le controller
            contacts.clear();
            contacts.addAll(controller.getContacts());
        });

        // Bouton d'ajout de contact
        addContactButton = new Button("+");
        addContactButton.getStyleClass().add("add-contact-btn");
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
                            cell.getStyleClass().add("new-message-highlight");

                            // Rétablir le style après un certain temps
                            new java.util.Timer().schedule(
                                    new java.util.TimerTask() {
                                        @Override
                                        public void run() {
                                            Platform.runLater(() -> cell.getStyleClass().remove("new-message-highlight"));
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
     * Mettre à jour l'indicateur d'appel pour un contact
     * @param contactUsername le nom d'utilisateur du contact
     * @param inCall true si un appel est en cours avec ce contact
     */
    public void updateCallIndicator(String contactUsername, boolean inCall) {
        // Trouver l'élément de liste correspondant au contact
        Platform.runLater(() -> {
            // Parcourir les cellules visibles et mettre à jour l'indicateur
            // pour le contact spécifié
        });
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