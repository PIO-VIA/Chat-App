package org.personnal.client.network;

import com.google.gson.Gson;
import javafx.application.Platform;
import org.personnal.client.UI.ChatView;
import org.personnal.client.model.FileData;
import org.personnal.client.model.Message;
import org.personnal.client.protocol.PeerResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Classe qui écoute les messages entrants du serveur en arrière-plan
 */
public class MessageListener extends Thread {
    private final BufferedReader input;
    private final ChatView chatView;
    private final String currentUsername;
    private boolean running = true;
    private final Gson gson = new Gson();

    public MessageListener(BufferedReader input, ChatView chatView, String currentUsername) {
        this.input = input;
        this.chatView = chatView;
        this.currentUsername = currentUsername;
        setDaemon(true); // Thread en arrière-plan qui s'arrête quand l'application se ferme
    }

    @Override
    public void run() {
        System.out.println("MessageListener démarré pour l'utilisateur " + currentUsername);

        while (running) {
            try {
                String responseJson = input.readLine();
                if (responseJson == null) {
                    // La connexion a été fermée
                    System.out.println("Connexion au serveur perdue.");
                    break;
                }

                // Traiter la réponse du serveur
                PeerResponse response = gson.fromJson(responseJson, PeerResponse.class);

                if (response.isSuccess()) {
                    // Traiter selon le message reçu
                    if (response.getMessage().contains("message reçu")) {
                        handleNewMessage(response);
                    } else if (response.getMessage().contains("fichier")) {
                        handleNewFile(response);
                    }
                }

            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture des messages: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("Erreur inattendue dans MessageListener: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("MessageListener arrêté pour l'utilisateur " + currentUsername);
    }

    /**
     * Traite un nouveau message reçu
     */
    private void handleNewMessage(PeerResponse response) {
        try {
            Object data = response.getData();
            Message message = null;

            if (data instanceof Map) {
                // Si la réponse contient directement un message sous forme de Map
                Map<String, Object> messageData = (Map<String, Object>) data;

                String sender = null;
                String content = null;
                String receiver = currentUsername;

                // Gérer les différents formats possibles
                if (messageData.containsKey("from")) {
                    sender = (String) messageData.get("from");
                } else if (messageData.containsKey("sender")) {
                    sender = (String) messageData.get("sender");
                }

                if (messageData.containsKey("content")) {
                    content = (String) messageData.get("content");
                }

                if (sender != null && content != null) {
                    message = new Message();
                    message.setSender(sender);
                    message.setReceiver(receiver);
                    message.setContent(content);
                    message.setTimestamp(LocalDateTime.now());
                    message.setRead(false);

                    System.out.println("Message reçu de " + sender + ": " + content);
                }
            } else {
                // Tenter de convertir directement l'objet en Message
                try {
                    message = gson.fromJson(gson.toJson(data), Message.class);
                    System.out.println("Message reçu (objet): " + message.getSender() + ": " + message.getContent());
                } catch (Exception e) {
                    System.err.println("Impossible de convertir en Message: " + e.getMessage());
                }
            }

            if (message != null) {
                // Variable finale pour utilisation dans le lambda
                final Message finalMessage = message;

                // Afficher le message dans l'interface graphique
                Platform.runLater(() -> chatView.addMessageToConversation(finalMessage));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du nouveau message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Traite un nouveau fichier reçu
     */
    private void handleNewFile(PeerResponse response) {
        try {
            Map<String, String> fileData = (Map<String, String>) response.getData();
            String sender = fileData.get("from");
            String filename = fileData.get("filename");
            String content = fileData.get("content");

            FileData file = new FileData();
            file.setSender(sender);
            file.setReceiver(currentUsername);
            file.setFilename(filename);
            file.setTimestamp(LocalDateTime.now());
            file.setRead(false);

            // Sauvegarder le contenu du fichier localement (optionnel)
            // saveFileContent(filename, content);

            // Afficher le fichier dans l'interface graphique
            Platform.runLater(() -> chatView.addFileToConversation(file));

        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du nouveau fichier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Arrête l'écoute des messages
     */
    public void stopListening() {
        running = false;
        interrupt();
    }
}