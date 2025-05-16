package org.personnal.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import org.personnal.client.MainClient;
import org.personnal.client.model.Message;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ChatController {
    private final MainClient app;
    private final String currentUsername;
    private final ClientSocketManager socketManager;
    private final ObservableList<String> contacts = FXCollections.observableArrayList();
    private String currentChatPartner;

    public ChatController(MainClient app, String currentUsername) throws IOException {
        this.app = app;
        this.currentUsername = currentUsername;
        this.socketManager = ClientSocketManager.getInstance();

    }



    /**
     * Envoie un message à l'utilisateur actuellement sélectionné
     */
    public boolean sendMessage(String receiver, String content) {
        if (receiver == null || receiver.isEmpty() || content == null || content.isEmpty()) {
            return false;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("sender", currentUsername);
            payload.put("receiver", receiver);
            payload.put("content", content);
            payload.put("read", "false");

            PeerRequest request = new PeerRequest(RequestType.SEND_MESSAGE, payload);
            socketManager.sendRequest(request);
            PeerResponse response = socketManager.readResponse();

            return response.isSuccess();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Charge les messages pour un contact spécifique
     */
    public ObservableList<Message> loadMessagesForContact(String contact) {
        ObservableList<Message> messages = FXCollections.observableArrayList();
        // TODO: Implémenter la récupération des messages depuis le serveur
        return messages;
    }

    /**
     * Gère l'envoi de fichier
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
        // Implémentation déplacée depuis ChatView
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

        if (!response.isSuccess()) {
            throw new IOException("Échec de l'envoi: " + response.getMessage());
        }
    }



    /**
     * Ajoute un nouveau contact
     */
    public boolean addContact(String username) {
        if (username == null || username.isEmpty() || username.equals(currentUsername) || contacts.contains(username)) {
            return false;
        }

        // TODO: Vérifier avec le serveur si l'utilisateur existe
        contacts.add(username);
        return true;
    }

    /**
     * Filtre les contacts selon le texte de recherche
     */
    public void filterContacts(String searchText) {
        // Le filtrage est géré automatiquement par l'ObservableList et le binding dans la vue
    }

    /**
     * Vérifie si un utilisateur est en ligne
     */
    public boolean isUserOnline(String username) {
        // TODO: Implémenter une vérification réelle avec le serveur
        return contacts.contains(username);
    }

    /**
     * Déconnecte l'utilisateur
     */
    public void disconnect() {
        try {
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

    public void setCurrentChatPartner(String partner) {
        this.currentChatPartner = partner;
    }
}