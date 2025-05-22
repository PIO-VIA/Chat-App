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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
        // Désactiver complètement les barres de séparation entre les éléments
        contactListView.setCellFactory(createContactCellFactory());
        contactListView.setStyle("-fx-background-insets: 0; -fx-padding: 0; -fx-border-width: 0; -fx-background-color: white;");
        contactListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            // Supprimer toute ligne persistante après sélection
            Platform.runLater(() -> {
                // Forcer le rafraîchissement de la vue pour éliminer les lignes
                contactListView.refresh();
            });
        });

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
                    setStyle("-fx-border-width: 0; -fx-border-color: transparent;");
                } else {
                    getStyleClass().add("contact-cell");
                    setStyle("-fx-border-width: 0; -fx-border-color: transparent;");

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

                    // *** OPTIMISATION : Récupération du dernier message en cache ***
                    String lastMessage = getCachedLastMessage(contact);

                    Label lastMessageLabel = new Label(lastMessage);
                    lastMessageLabel.getStyleClass().add("contact-message");

                    contactInfo.getChildren().addAll(contactName, lastMessageLabel);

                    // *** STATUT EN LIGNE OPTIMISÉ : Utilise uniquement le cache ***
                    Circle onlineStatus = new Circle(5);

                    // Ne pas faire de requête réseau ici, utiliser seulement le cache du controller
                    boolean isOnline = getCachedOnlineStatus(contact);

                    if (isOnline) {
                        onlineStatus.getStyleClass().add("online-status");
                    } else {
                        onlineStatus.getStyleClass().add("offline-status");
                    }

                    VBox.setMargin(onlineStatus, new Insets(5, 0, 0, 0));

                    // *** INDICATEUR DE MESSAGES NON LUS OPTIMISÉ ***
                    boolean hasUnreadMessages = getCachedUnreadStatus(contact);

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
    private final Map<String, String> lastMessageCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> unreadStatusCache = new ConcurrentHashMap<>();

    /**
     * *** RÉCUPÉRATION OPTIMISÉE DU DERNIER MESSAGE ***
     * Utilise un cache local pour éviter les requêtes fréquentes à la BD
     */
    private String getCachedLastMessage(String contact) {
        // Vérifier le cache d'abord
        String cachedMessage = lastMessageCache.get(contact);
        if (cachedMessage != null) {
            return cachedMessage;
        }

        // Si pas en cache, récupérer de la BD en arrière-plan
        CompletableFuture.runAsync(() -> {
            try {
                List<Message> contactMessages = controller.getMessageDAO().getMessagesWith(contact);
                String lastMessage = "";

                if (!contactMessages.isEmpty()) {
                    Message lastMsg = contactMessages.get(contactMessages.size() - 1);
                    lastMessage = lastMsg.getContent();

                    // Tronquer le message s'il est trop long
                    if (lastMessage.length() > 30) {
                        lastMessage = lastMessage.substring(0, 27) + "...";
                    }
                }

                // Mettre en cache
                lastMessageCache.put(contact, lastMessage);

                // Rafraîchir l'UI si nécessaire
                Platform.runLater(() -> contactListView.refresh());

            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération du dernier message pour " + contact + ": " + e.getMessage());
            }
        });

        return ""; // Retourner vide en attendant
    }

    /**
     * *** RÉCUPÉRATION OPTIMISÉE DU STATUT EN LIGNE ***
     * Utilise uniquement le cache du controller, pas de requête réseau
     */
    private boolean getCachedOnlineStatus(String contact) {
        // Utiliser uniquement le cache du controller, ne pas déclencher de requête réseau
        return controller.isUserOnline(contact);
    }

    /**
     * *** RÉCUPÉRATION OPTIMISÉE DU STATUT DE MESSAGES NON LUS ***
     * Utilise un cache local pour éviter les requêtes fréquentes à la BD
     */
    private boolean getCachedUnreadStatus(String contact) {
        // Vérifier le cache d'abord
        Boolean cached = unreadStatusCache.get(contact);
        if (cached != null) {
            return cached;
        }

        // Si pas en cache, récupérer en arrière-plan
        CompletableFuture.runAsync(() -> {
            try {
                boolean hasUnread = controller.getMessageDAO().hasUnreadMessagesFrom(contact);
                unreadStatusCache.put(contact, hasUnread);

                // Rafraîchir l'UI si nécessaire
                if (hasUnread) {
                    Platform.runLater(() -> contactListView.refresh());
                }

            } catch (Exception e) {
                System.err.println("Erreur lors de la vérification des messages non lus pour " + contact + ": " + e.getMessage());
                unreadStatusCache.put(contact, false);
            }
        });

        return false; // Retourner false en attendant
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
        refreshContactsButton = new Button("🔄 R");
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

        // Lancer le rafraîchissement en arrière-plan avec la nouvelle méthode optimisée
        CompletableFuture.runAsync(() -> {
            try {
                // Utiliser la méthode optimisée du controller
                Map<String, Boolean> statuses = controller.refreshContactStatuses();

                // Vider les caches locaux pour forcer le rafraîchissement
                lastMessageCache.clear();
                unreadStatusCache.clear();

                // Mettre à jour l'interface
                Platform.runLater(() -> {
                    // Rafraîchir la liste
                    contactListView.refresh();

                    // Réactiver le bouton et masquer l'indicateur
                    refreshContactsButton.setDisable(false);
                    refreshProgress.setVisible(false);

                    System.out.println("Statuts rafraîchis pour " + statuses.size() + " contacts");
                });
            } catch (Exception e) {
                System.err.println("Erreur lors du rafraîchissement des statuts: " + e.getMessage());

                // Réactiver le bouton et masquer l'indicateur en cas d'erreur
                Platform.runLater(() -> {
                    refreshContactsButton.setDisable(false);
                    refreshProgress.setVisible(false);
                });
            }
        });
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
        // Mettre à jour le cache du dernier message
        String truncatedMessage = lastMessage;
        if (truncatedMessage.length() > 30) {
            truncatedMessage = truncatedMessage.substring(0, 27) + "...";
        }
        lastMessageCache.put(contact, truncatedMessage);

        // Mettre à jour le cache des messages non lus
        if (!contact.equals(currentChatPartner)) {
            unreadStatusCache.put(contact, true);
        }

        // Rafraîchir l'affichage
        Platform.runLater(() -> {
            contactListView.refresh();

            // Animation de surbrillance pour les nouveaux messages (optionnel)
            if (!contact.equals(currentChatPartner)) {
                // Logique d'animation existante...
            }
        });

        // Remonter le contact en haut de la liste si ce n'est pas la conversation active
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
    /**
     * *** RAFRAÎCHISSEMENT OPTIMISÉ DE LA LISTE ***
     * Vide les caches et rafraîchit l'affichage
     */
    public void refreshContactList() {
        // Vider les caches pour forcer le rafraîchissement
        lastMessageCache.clear();
        unreadStatusCache.clear();

        // Rafraîchir l'affichage
        contactListView.refresh();
    }
    /**
     * Retourne le panneau des contacts
     */
    public BorderPane getPanel() {
        return panel;
    }

}