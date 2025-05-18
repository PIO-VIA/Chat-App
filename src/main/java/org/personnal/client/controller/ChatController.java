package org.personnal.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import org.personnal.client.MainClient;
import org.personnal.client.database.DAO.*;
import org.personnal.client.model.FileData;
import org.personnal.client.model.Message;
import org.personnal.client.model.User;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;
import org.personnal.client.UI.ChatView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatController {
    private final MainClient app;
    private final String currentUsername;
    private final ClientSocketManager socketManager;
    private final ObservableList<String> contacts = FXCollections.observableArrayList();
    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    private String currentChatPartner;

    // DAOs pour accéder à la base de données locale
    private final IUserDAO userDAO;
    private final IMessageDAO messageDAO;
    private final IFileDAO fileDAO;

    // Référence à la vue de chat (nécessaire pour le MessageListener)
    private ChatView chatView;

    // Planificateur pour vérifier régulièrement l'état du listener
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Cache des utilisateurs pour éviter des requêtes répétées à la base de données
    private final Map<String, User> userCache = new ConcurrentHashMap<>();

    // Drapeau pour éviter les mises à jour UI trop fréquentes
    private volatile boolean refreshingContactList = false;

    public ChatController(MainClient app, String currentUsername) throws IOException {
        this.app = app;
        this.currentUsername = currentUsername;
        this.socketManager = ClientSocketManager.getInstance();

        // Initialiser les DAOs
        this.userDAO = new UserDAO();
        this.messageDAO = new MessageDAO();
        this.fileDAO = new FileDAO();

        // Définir l'utilisateur courant comme propriété système pour les requêtes SQL
        System.setProperty("current.user", currentUsername);

        // Charger les contacts depuis la BD locale au démarrage
        loadContactsFromDatabase();
    }

    /**
     * Configure la vue de chat et démarre le listener de messages
     * @param chatView Vue de chat à configurer
     */
    public void setupChatView(ChatView chatView) {
        this.chatView = chatView;

        // Démarrer le listener de messages
        socketManager.startMessageListener(chatView, currentUsername);

        // Planifier une vérification régulière du listener (moins fréquente, 30 secondes)
        scheduler.scheduleAtFixedRate(this::checkMessageListener, 30, 30, TimeUnit.SECONDS);

        System.out.println("ChatView configurée pour l'utilisateur " + currentUsername);
    }

    /**
     * Vérifie l'état du MessageListener et le redémarre si nécessaire
     */
    private void checkMessageListener() {
        if (!socketManager.isMessageListenerRunning() && chatView != null) {
            System.out.println("MessageListener n'est plus en exécution, redémarrage...");

            // Redémarrer sur le thread JavaFX
            Platform.runLater(() -> {
                try {
                    // Redémarrer le listener sans notification pour éviter de perturber l'utilisateur
                    socketManager.startMessageListener(chatView, currentUsername);
                } catch (Exception e) {
                    System.err.println("Erreur lors du redémarrage du MessageListener: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Affiche une notification à l'utilisateur
     */
    private void showNotification(String message) {
        Platform.runLater(() -> {
            try {
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
            } catch (Exception e) {
                System.err.println("Erreur lors de l'affichage de la notification: " + e.getMessage());
            }
        });
    }

    /**
     * Charge les contacts de l'utilisateur depuis la base de données locale
     */
    private void loadContactsFromDatabase() {
        // Vider les listes avant de les recharger
        contacts.clear();
        usersList.clear();
        userCache.clear();

        // Récupérer tous les utilisateurs depuis la base de données
        List<User> contactList = userDAO.findAll();

        for (User contact : contactList) {
            contacts.add(contact.getUsername());
            usersList.add(contact);
            userCache.put(contact.getUsername(), contact);
        }
    }

    /**
     * Envoie un message à l'utilisateur actuellement sélectionné et le sauvegarde en local
     */
    public boolean sendMessage(String receiver, String content) {
        if (receiver == null || receiver.isEmpty() || content == null || content.isEmpty()) {
            return false;
        }

        try {
            // Préparer le message pour l'envoi au serveur
            Map<String, String> payload = new HashMap<>();
            payload.put("sender", currentUsername);
            payload.put("receiver", receiver);
            payload.put("content", content);
            payload.put("read", "false");

            // Créer un objet Message pour l'interface (optimiste)
            Message message = new Message();
            message.setSender(currentUsername);
            message.setReceiver(receiver);
            message.setContent(content);
            message.setTimestamp(LocalDateTime.now());
            message.setRead(true); // Les messages que nous envoyons sont considérés comme lus

            // Ajouter le message à la vue immédiatement (UI plus réactive)
            if (chatView != null) {
                chatView.addMessageToConversation(message);
            }

            // Envoyer le message au serveur en arrière-plan
            new Thread(() -> {
                try {
                    // Envoyer la requête
                    PeerRequest request = new PeerRequest(RequestType.SEND_MESSAGE, payload);
                    socketManager.sendRequest(request);
                    PeerResponse response = socketManager.readResponse();

                    if (response.isSuccess()) {
                        // Sauvegarder le message dans la BD locale
                        messageDAO.saveMessage(message);
                    } else {
                        // Afficher une notification d'erreur
                        showNotification("Erreur lors de l'envoi du message: " + response.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
                    showNotification("Problème de connexion. Le message sera réessayé plus tard.");
                }
            }).start();

            return true;
        } catch (Exception e) {
            System.err.println("Erreur grave lors de l'envoi du message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Récupère un contact par son nom d'utilisateur (avec cache)
     */
    public User getUserByUsername(String username) {
        // Vérifier le cache d'abord
        User cachedUser = userCache.get(username);
        if (cachedUser != null) {
            return cachedUser;
        }

        // Sinon, chercher dans la BD et mettre en cache
        User user = userDAO.findByUsername(username);
        if (user != null) {
            userCache.put(username, user);
        }
        return user;
    }

    /**
     * Charge les messages pour un contact spécifique depuis la base de données locale
     */
    public ObservableList<Message> loadMessagesForContact(String contact) {
        ObservableList<Message> messages = FXCollections.observableArrayList();

        try {
            // Définir l'utilisateur courant comme propriété système pour la requête SQL
            System.setProperty("current.user", currentUsername);

            // Récupérer les messages échangés avec ce contact depuis la BD locale
            List<Message> messageList = messageDAO.getMessagesWith(contact);
            messages.addAll(messageList);

            // Récupérer également les fichiers échangés avec ce contact
            List<FileData> fileList = fileDAO.getFilesWith(contact);

            // Convertir les fichiers en messages pour les afficher dans la conversation
            for (FileData file : fileList) {
                Message fileMessage = new Message();
                fileMessage.setIdMessage(file.getId());
                fileMessage.setSender(file.getSender());
                fileMessage.setReceiver(file.getReceiver());
                fileMessage.setContent("Fichier: " + file.getFilename());
                fileMessage.setTimestamp(file.getTimestamp());
                fileMessage.setRead(file.isRead());

                messages.add(fileMessage);
            }

            // Trier les messages par date
            messages.sort((m1, m2) -> {
                if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
                if (m1.getTimestamp() == null) return -1;
                if (m2.getTimestamp() == null) return 1;
                return m1.getTimestamp().compareTo(m2.getTimestamp());
            });

            // Marquer les messages du contact comme lus
            messageDAO.markMessagesAsRead(contact, currentUsername);

            // Marquer également les fichiers comme lus
            if (fileDAO instanceof FileDAO) {
                ((FileDAO) fileDAO).markFilesAsRead(contact, currentUsername);
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des messages: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Gère l'envoi de fichier et le sauvegarde en local
     */
    public void handleAttachment() {
        if (currentChatPartner == null) {
            showNotification("Veuillez sélectionner un contact avant d'envoyer un fichier.");
            return;
        }

        File file = chooseFile();
        if (file != null) {
            // Vérifier la taille du fichier
            if (file.length() > 5 * 1024 * 1024) { // 5MB
                showNotification("Le fichier est trop volumineux. Taille maximale: 5MB");
                return;
            }

            // Créer un FileData pour l'affichage immédiat (UI réactive)
            FileData fileData = new FileData();
            fileData.setSender(currentUsername);
            fileData.setReceiver(currentChatPartner);
            fileData.setFilename(file.getName());
            fileData.setFilepath(file.getAbsolutePath());
            fileData.setTimestamp(LocalDateTime.now());
            fileData.setRead(true);

            // Ajouter immédiatement à l'UI
            if (chatView != null) {
                chatView.addFileToConversation(fileData);
            }

            // Envoi en arrière-plan
            new Thread(() -> {
                try {
                    sendFileInBackground(currentChatPartner, file, fileData);
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'envoi du fichier: " + e.getMessage());
                    Platform.runLater(() ->
                            showNotification("Erreur lors de l'envoi du fichier: " + e.getMessage())
                    );
                }
            }).start();
        }
    }

    private File chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier à envoyer");
        return fileChooser.showOpenDialog(null);
    }

    private void sendFileInBackground(String receiver, File file, FileData fileData) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        String base64Content = Base64.getEncoder().encodeToString(fileContent);

        Map<String, String> payload = new HashMap<>();
        payload.put("sender", currentUsername);
        payload.put("receiver", receiver);
        payload.put("filename", file.getName());
        payload.put("content", base64Content);

        PeerRequest request = new PeerRequest(RequestType.SEND_FILE, payload);
        socketManager.sendRequest(request);
        PeerResponse response = socketManager.readResponse();

        if (response.isSuccess()) {
            // Sauvegarder le fichier dans la BD locale
            fileDAO.saveFile(fileData);
        } else {
            throw new IOException("Échec de l'envoi: " + response.getMessage());
        }
    }

    /**
     * Vérifie si un utilisateur existe sur le serveur
     * @param username Le nom d'utilisateur à vérifier
     * @return true si l'utilisateur existe, false sinon
     */
    public boolean checkUserExists(String username) {
        if (username == null || username.isEmpty() || username.equals(currentUsername)) {
            return false;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);

            PeerRequest request = new PeerRequest(RequestType.CHECK_USER, payload);
            socketManager.sendRequest(request);
            PeerResponse response = socketManager.readResponse();

            return response.isSuccess();
        } catch (IOException e) {
            System.err.println("Erreur lors de la vérification de l'utilisateur: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ajoute un nouveau contact à la base de données locale
     * @param username Le nom d'utilisateur à ajouter comme contact
     * @param email L'email du contact
     * @return true si l'ajout a réussi, false sinon
     */
    public boolean addContact(String username, String email) {
        // Vérification de base
        if (username == null || username.isEmpty() || username.equals(currentUsername) || contacts.contains(username)) {
            return false;
        }

        // Vérifier si l'utilisateur existe sur le serveur
        if (!checkUserExists(username)) {
            return false;
        }

        // Ajouter à la base de données locale
        User newContact = new User(username, email);
        try {
            userDAO.insert(newContact);

            // Mettre à jour les listes observables pour l'interface
            contacts.add(username);
            usersList.add(newContact);
            userCache.put(username, newContact);

            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout du contact : " + e.getMessage());
            return false;
        }
    }

    /**
     * Supprime un contact de la base de données locale
     */
    public boolean deleteContact(int userId) {
        try {
            User user = userDAO.findById(userId);
            if (user != null) {
                // Supprimer le contact de la base de données
                userDAO.delete(userId);

                // Mettre à jour les listes observables pour l'interface
                contacts.remove(user.getUsername());
                usersList.removeIf(u -> u.getIdUser() == userId);
                userCache.remove(user.getUsername());

                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression du contact : " + e.getMessage());
            return false;
        }
    }

    /**
     * Filtre les contacts selon le texte de recherche
     */
    public void filterContacts(String searchText) {
        // Pour éviter plusieurs opérations simultanées
        if (refreshingContactList) return;

        refreshingContactList = true;

        try {
            // Recharger tous les contacts si le texte de recherche est vide
            if (searchText == null || searchText.isEmpty()) {
                loadContactsFromDatabase();
                return;
            }

            // Sinon, filtrer les contacts par nom d'utilisateur ou email
            String searchLower = searchText.toLowerCase();
            contacts.clear();
            usersList.clear();

            List<User> allContacts = userDAO.findAll();
            for (User user : allContacts) {
                if (user.getUsername().toLowerCase().contains(searchLower) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower))) {
                    contacts.add(user.getUsername());
                    usersList.add(user);
                }
            }
        } finally {
            refreshingContactList = false;
        }
    }

    /**
     * Vérifie si un utilisateur est en ligne (délégué au SocketManager pour le cache)
     */
    public boolean isUserOnline(String username) {
        return socketManager.isUserOnline(username);
    }

    /**
     * Enregistre un message reçu dans la base de données locale
     */
    public void saveReceivedMessage(Message message) {
        try {
            // Marquer le message comme non lu puisqu'il vient d'être reçu
            message.setRead(false);
            messageDAO.saveMessage(message);

            // Si la vue est disponible et que le message vient du partenaire de chat actuel,
            // rafraîchir la liste des messages
            if (chatView != null && currentChatPartner != null &&
                    message.getSender().equals(currentChatPartner)) {

                // Marquer le message comme lu immédiatement
                messageDAO.markMessagesAsRead(currentChatPartner, currentUsername);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde du message reçu: " + e.getMessage());
        }
    }

    /**
     * Enregistre un fichier reçu dans la base de données locale
     */
    public void saveReceivedFile(FileData file) {
        try {
            // Marquer le fichier comme non lu puisqu'il vient d'être reçu
            file.setRead(false);
            fileDAO.saveFile(file);

            // Si la vue est disponible et que le fichier vient du partenaire de chat actuel,
            // le marquer comme lu
            if (chatView != null && currentChatPartner != null &&
                    file.getSender().equals(currentChatPartner) &&
                    fileDAO instanceof FileDAO) {

                ((FileDAO) fileDAO).markFilesAsRead(currentChatPartner, currentUsername);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde du fichier reçu: " + e.getMessage());
        }
    }

    /**
     * Déconnecte l'utilisateur
     */
    public void disconnect() {
        try {
            // Arrêter le planificateur
            scheduler.shutdownNow();

            // Arrêter le listener de messages avant de fermer la connexion
            socketManager.stopMessageListener();

            // Envoyer la requête de déconnexion
            try {
                PeerRequest request = new PeerRequest(RequestType.DISCONNECT, null);
                socketManager.sendRequest(request);
                socketManager.readResponse();
            } catch (Exception e) {
                // Ignorer les erreurs lors de la déconnexion
            }

            // Fermer la connexion
            socketManager.closeConnection();
        } catch (Exception e) {
            System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    // Getters
    public String getCurrentUsername() {
        return currentUsername;
    }

    public ObservableList<String> getContacts() {
        return contacts;
    }

    public ObservableList<User> getUsersList() {
        return usersList;
    }

    public void setCurrentChatPartner(String partner) {
        this.currentChatPartner = partner;
    }

    public IUserDAO getUserDAO() {
        return userDAO;
    }

    public IMessageDAO getMessageDAO() {
        return messageDAO;
    }

    public IFileDAO getFileDAO() {
        return fileDAO;
    }
}