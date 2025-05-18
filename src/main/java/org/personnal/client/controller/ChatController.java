package org.personnal.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

        System.out.println("ChatView configurée pour l'utilisateur " + currentUsername);
    }

    /**
     * Charge les contacts de l'utilisateur depuis la base de données locale
     */
    private void loadContactsFromDatabase() {
        // Vider les listes avant de les recharger
        contacts.clear();
        usersList.clear();

        // Récupérer tous les utilisateurs depuis la base de données
        List<User> contactList = userDAO.findAll();

        for (User contact : contactList) {
            contacts.add(contact.getUsername());
            usersList.add(contact);
        }

        System.out.println("Contacts chargés depuis la base de données: " + contacts.size());
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

            // Envoyer le message au serveur
            PeerRequest request = new PeerRequest(RequestType.SEND_MESSAGE, payload);
            socketManager.sendRequest(request);
            PeerResponse response = socketManager.readResponse();

            if (response.isSuccess()) {
                // Créer un objet Message et l'enregistrer dans la BD locale
                Message message = new Message();
                message.setSender(currentUsername);
                message.setReceiver(receiver);
                message.setContent(content);
                message.setTimestamp(LocalDateTime.now());
                message.setRead(true); // Les messages que nous envoyons sont considérés comme lus

                // Sauvegarder le message dans la BD locale
                messageDAO.saveMessage(message);

                System.out.println("Message envoyé à " + receiver + " et sauvegardé localement");

                return true;
            }
            return false;
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Récupère un contact par son nom d'utilisateur
     */
    public User getUserByUsername(String username) {
        return userDAO.findByUsername(username);
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

            if (messageList.isEmpty()) {
                System.out.println("Aucun message trouvé avec " + contact);
            } else {
                System.out.println("Messages trouvés: " + messageList.size());

                // Pour déboguer, afficher quelques messages
                for (int i = 0; i < Math.min(messageList.size(), 3); i++) {
                    Message msg = messageList.get(i);
                    System.out.println("Message " + i + ": " + msg.getSender() + " -> " +
                            msg.getReceiver() + ": " + msg.getContent() +
                            " (" + msg.getTimestamp() + ")");
                }
            }

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

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des messages: " + e.getMessage());
            e.printStackTrace();
        }

        return messages;
    }

    /**
     * Gère l'envoi de fichier et le sauvegarde en local
     */
    public void handleAttachment() {
        File file = chooseFile();
        if (file != null && currentChatPartner != null) {
            try {
                sendFile(currentChatPartner, file);
            } catch (IOException e) {
                System.err.println("Erreur lors de l'envoi du fichier: " + e.getMessage());
            }
        }
    }

    private File chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier à envoyer");
        return fileChooser.showOpenDialog(null);
    }

    private void sendFile(String receiver, File file) throws IOException {
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
            FileData fileData = new FileData();
            fileData.setSender(currentUsername);
            fileData.setReceiver(receiver);
            fileData.setFilename(file.getName());
            fileData.setFilepath(file.getAbsolutePath());
            fileData.setTimestamp(LocalDateTime.now());
            fileData.setRead(true); // Les fichiers que nous envoyons sont considérés comme lus

            fileDAO.saveFile(fileData);

            System.out.println("Fichier envoyé à " + receiver + " et sauvegardé localement: " + file.getName());
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

            if (response.isSuccess()) {
                // L'utilisateur existe sur le serveur
                return true;
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la vérification de l'utilisateur: " + e.getMessage());
        }
        return false;
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
        boolean saved;
        try {
            userDAO.insert(newContact);
            saved = true;

            // Mettre à jour les listes observables pour l'interface
            contacts.add(username);
            usersList.add(newContact);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout du contact : " + e.getMessage());
            saved = false;
        }

        return saved;
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
    }

    /**
     * Vérifie si un utilisateur est en ligne
     */
    public boolean isUserOnline(String username) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);

            PeerRequest request = new PeerRequest(RequestType.CHECK_ONLINE, payload);
            socketManager.sendRequest(request);
            PeerResponse response = socketManager.readResponse();

            if (response.isSuccess()) {
                Map<String, String> data = (Map<String, String>) response.getData();
                return "true".equals(data.get("online"));
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la vérification du statut: " + e.getMessage());
        }
        return false;
    }

    /**
     * Enregistre un message reçu dans la base de données locale
     */
    public void saveReceivedMessage(Message message) {
        try {
            // Marquer le message comme non lu puisqu'il vient d'être reçu
            message.setRead(false);
            messageDAO.saveMessage(message);

            System.out.println("Message reçu de " + message.getSender() + " sauvegardé dans la BD: " + message.getContent());

            // Si la vue est disponible et que le message vient du partenaire de chat actuel,
            // rafraîchir la liste des messages
            if (chatView != null && currentChatPartner != null &&
                    message.getSender().equals(currentChatPartner)) {

                chatView.refreshMessages();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde du message reçu: " + e.getMessage());
            e.printStackTrace();
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

            System.out.println("Fichier reçu de " + file.getSender() + " sauvegardé dans la BD: " + file.getFilename());

            // Si la vue est disponible et que le fichier vient du partenaire de chat actuel,
            // rafraîchir la liste des messages
            if (chatView != null && currentChatPartner != null &&
                    file.getSender().equals(currentChatPartner)) {

                chatView.refreshMessages();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde du fichier reçu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Déconnecte l'utilisateur
     */
    public void disconnect() {
        try {
            // Arrêter le listener de messages avant de fermer la connexion
            socketManager.stopMessageListener();

            PeerRequest request = new PeerRequest(RequestType.DISCONNECT, null);
            socketManager.sendRequest(request);
            socketManager.closeConnection();
        } catch (IOException e) {
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