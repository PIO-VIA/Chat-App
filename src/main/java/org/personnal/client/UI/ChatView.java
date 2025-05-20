package org.personnal.client.UI;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.personnal.client.UI.call.CallWindow;
import org.personnal.client.call.AudioCallManager;
import org.personnal.client.controller.ChatController;
import org.personnal.client.model.FileData;
import org.personnal.client.model.Message;
import org.personnal.client.model.User;
import org.personnal.client.UI.components.ContactsPanel;
import org.personnal.client.UI.components.MessagesPanel;
import org.personnal.client.UI.dialogs.DialogManager;

import java.io.IOException;
import java.util.Map;

/**
 * Vue principale de l'application de chat
 * Coordonne les composants de la liste des contacts et des messages
 * Avec application des styles CSS
 */
public class ChatView {
    // Composants principaux de l'interface
    private final BorderPane mainView;
    private final SplitPane splitPane;
    private final ChatController controller;

    // Sous-composants
    private final ContactsPanel contactsPanel;
    private final MessagesPanel messagesPanel;
    private final DialogManager dialogManager;

    // Listes observables partagées
    private final ObservableList<String> contacts = FXCollections.observableArrayList();
    private final ObservableList<Message> messages = FXCollections.observableArrayList();

    // État actuel
    private String currentChatPartner = null;

    // Constantes
    private static final double LEFT_PANEL_WIDTH = 300;
    private static final double RIGHT_PANEL_MIN_WIDTH = 400;

    private AudioCallManager audioCallManager;
    /**
     * Constructeur de la vue de chat
     * @param controller Contrôleur de chat
     */
    public ChatView(ChatController controller) {
        this.controller = controller;
        this.controller.setCurrentChatPartner(null);
        this.dialogManager = new DialogManager(controller, new Stage());
        // Initialiser le gestionnaire d'appels
        try {
            this.audioCallManager = new AudioCallManager(controller);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'initialisation du gestionnaire d'appels: " + e.getMessage());
        }

        // Initialiser les composants principaux
        mainView = new BorderPane();
        mainView.getStyleClass().add("main-view");

        splitPane = new SplitPane();
        splitPane.getStyleClass().add("chat-split-pane");

        // Initialiser les panneaux
        contactsPanel = new ContactsPanel(controller, contacts, this::switchConversation,
                this.dialogManager::showAddContactDialog,
                this.dialogManager::showSettingsDialog);

        messagesPanel = new MessagesPanel(controller, messages, this::sendMessage, this::initiateCall);

        setupSplitPane();

        // Configuration globale
        mainView.setCenter(splitPane);

        // Charger les contacts depuis la BD locale
        updateContactList();

        // Configurer le controller avec cette vue
        controller.setupChatView(this);
    }

    /**
     * Configure le SplitPane qui divise les contacts et les messages
     */
    private void setupSplitPane() {
        splitPane.getItems().addAll(contactsPanel.getPanel(), messagesPanel.getPanel());
        splitPane.setDividerPositions(0.3);
        SplitPane.setResizableWithParent(contactsPanel.getPanel(), false);

        contactsPanel.getPanel().setPrefWidth(LEFT_PANEL_WIDTH);
        messagesPanel.getPanel().setMinWidth(RIGHT_PANEL_MIN_WIDTH);
    }

    /**
     * Change la conversation active
     * @param contact Contact avec lequel discuter
     */
    public void switchConversation(String contact) {
        // Changer le partenaire de discussion actuel
        controller.setCurrentChatPartner(contact);
        currentChatPartner = contact;
        messagesPanel.setCurrentChatPartner(contact);

        // Charger les messages pour ce contact depuis la BD locale
        messages.clear();
        controller.loadMessagesForContact(contact).forEach(messages::add);

        // Afficher l'email du contact dans l'en-tête
        User user = controller.getUserByUsername(contact);
        if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
            messagesPanel.setContactHeader(contact + " (" + user.getEmail() + ")");
        } else {
            messagesPanel.setContactHeader(contact);
        }

        // Défilement vers le dernier message
        Platform.runLater(() -> {
            if (!messages.isEmpty()) {
                messagesPanel.scrollToLastMessage();
            }
        });
    }

    /**
     * Envoie un message au contact actuel
     */
    private void sendMessage() {
        String content = messagesPanel.getMessageText();
        if (!content.isEmpty() && currentChatPartner != null) {
            // Afficher l'indicateur d'envoi
            messagesPanel.showSendingIndicator(true);

            boolean success = controller.sendMessage(currentChatPartner, content);
            if (success) {
                messagesPanel.clearMessageInput();

                // Recharger la conversation pour afficher le nouveau message
                messages.clear();
                controller.loadMessagesForContact(currentChatPartner).forEach(messages::add);

                // Défilement vers le dernier message
                Platform.runLater(() -> {
                    if (!messages.isEmpty()) {
                        messagesPanel.scrollToLastMessage();
                    }

                    // Masquer l'indicateur d'envoi
                    messagesPanel.showSendingIndicator(false);
                });
            } else {
                // Masquer l'indicateur d'envoi en cas d'échec
                messagesPanel.showSendingIndicator(false);
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

            // Déterminer le partenaire
            String partner = message.getSender().equals(controller.getCurrentUsername())
                    ? message.getReceiver() : message.getSender();

            // Si c'est la conversation active, mettre à jour la vue
            if (partner.equals(currentChatPartner)) {
                messages.add(message);
                messagesPanel.scrollToLastMessage();
            }

            // Mettre à jour la cellule du contact et le positionner en haut de la liste
            contactsPanel.updateContactWithLastMessage(partner, message.getContent(), currentChatPartner);
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

            // Déterminer le partenaire
            String partner = file.getSender().equals(controller.getCurrentUsername())
                    ? file.getReceiver() : file.getSender();

            if (partner.equals(currentChatPartner)) {
                // Recharger tous les messages et fichiers pour ce contact
                refreshMessages();
            }

            // Mettre à jour la cellule du contact
            contactsPanel.updateContactWithLastMessage(partner, "Fichier: " + file.getFilename(), currentChatPartner);
        });
    }

    /**
     * Rafraîchit la liste des messages pour le contact actuel
     */
    public void refreshMessages() {
        if (currentChatPartner != null) {
            Platform.runLater(() -> {
                // Recharger tous les messages
                messages.clear();
                messages.addAll(controller.loadMessagesForContact(currentChatPartner));

                // Défiler vers le bas
                if (!messages.isEmpty()) {
                    messagesPanel.scrollToLastMessage();
                }
            });
        }
    }

    /**
     * Met à jour la liste des contacts depuis la BD locale
     */
    public void updateContactList() {
        Platform.runLater(() -> {
            contacts.clear();
            contacts.addAll(controller.getContacts());
            contactsPanel.refreshContactList();
        });
    }


    /**
     * Initie un appel vers le contact actuellement sélectionné
     */
    private void initiateCall() {
        if (currentChatPartner != null && audioCallManager != null) {
            if (audioCallManager.initiateCall(currentChatPartner)) {
                // Afficher la fenêtre d'appel améliorée
                CallWindow callWindow = new CallWindow(audioCallManager, controller, false, currentChatPartner);
                callWindow.show();
            } else {
                // Afficher une notification d'erreur si l'initiation a échoué
                showNotification("Impossible d'initier l'appel, veuillez réessayer.");
            }
        }
    }

    // Méthode d'assistance pour afficher des notifications
    private void showNotification(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Notification");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();

            // Fermer automatiquement après 3 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(alert::close);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    /**
     * Gère les événements d'appel reçus du serveur
     * @param eventData Les données de l'événement d'appel
     */
    public void handleCallEvent(Map<String, String> eventData) {
        if (audioCallManager == null) {
            return;
        }

        // Déléguer au gestionnaire d'appels
        audioCallManager.handleCallEvent(eventData);

        // Afficher l'interface d'appel si c'est un appel entrant
        if ("incoming-call".equals(eventData.get("action"))) {
            String caller = eventData.get("caller");
            Platform.runLater(() -> {
                // Utiliser la fenêtre d'appel améliorée
                CallWindow callWindow = new CallWindow(audioCallManager, controller, true, caller);
                callWindow.show();

                // Afficher également une notification d'appel
                showIncomingCallNotification(caller);
            });
        }
    }

    // Méthode d'assistance pour afficher une notification d'appel entrant
    private void showIncomingCallNotification(String caller) {
        try {
            // Créer une notification spéciale pour les appels entrants
            Alert notification = new Alert(Alert.AlertType.INFORMATION);
            notification.setTitle("Appel entrant");
            notification.setHeaderText("Appel de " + caller);
            notification.setContentText("Vous avez un appel audio entrant.");

            // Jouer un son de sonnerie (si disponible)
            // mediaPlayer.play();

            // Fermer après 5 secondes si l'utilisateur n'a pas répondu
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    Platform.runLater(notification::close);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            notification.show();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage de la notification d'appel : " + e.getMessage());
        }
    }
    /**
     * Ajoute un nouveau contact à la liste
     * @param contact Nom du contact à ajouter
     */
    public void addContact(String contact) {
        if (!contacts.contains(contact)) {
            Platform.runLater(() -> {
                contacts.add(contact);
                contactsPanel.refreshContactList();
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
            contactsPanel.refreshContactList();
        });
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
        Platform.runLater(() -> contactsPanel.refreshContactList());
    }

}