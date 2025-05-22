package org.personnal.client.network;

import com.google.gson.Gson;
import javafx.application.Platform;
import org.personnal.client.UI.ChatView;
import org.personnal.client.model.FileData;
import org.personnal.client.model.Message;
import org.personnal.client.protocol.PeerResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

/**
 * Classe qui écoute les messages entrants du serveur en arrière-plan
 * Version simplifiée pour améliorer les performances
 */
public class MessageListener extends Thread {
    private final BufferedReader input;
    private final ChatView chatView;
    private final String currentUsername;
    private volatile boolean running = true;
    private final Gson gson = new Gson();

    public MessageListener(BufferedReader input, ChatView chatView, String currentUsername) {
        this.input = input;
        this.chatView = chatView;
        this.currentUsername = currentUsername;
        setDaemon(true); // Thread en arrière-plan qui s'arrête quand l'application se ferme
        setName("MessageListener-" + currentUsername); // Nommer le thread pour faciliter le débogage
    }

    @Override
    public void run() {
        System.out.println("MessageListener démarré pour l'utilisateur " + currentUsername);

        while (running) {
            try {
                // Tenter de lire une ligne du serveur (bloquant jusqu'au timeout ou données reçues)
                String responseJson = input.readLine();

                // Vérifier si la connexion est fermée
                if (responseJson == null) {
                    System.out.println("Connexion au serveur perdue.");
                    break;
                }

                // Traiter la réponse du serveur
                processResponse(responseJson);

            } catch (SocketTimeoutException e) {
                // Simplement continuer d'écouter
                continue;
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erreur de lecture: " + e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        System.out.println("MessageListener arrêté pour l'utilisateur " + currentUsername);
    }

    /**
     * Traite la réponse JSON
     */
    // Ajout dans la méthode processResponse() de MessageListener.java

    private void processResponse(String responseJson) {
        try {
            PeerResponse response = gson.fromJson(responseJson, PeerResponse.class);

            // Traiter les événements d'appel
            if (response.getData() instanceof Map) {
                Map<String, String> dataMap = (Map<String, String>) response.getData();
                if (dataMap.containsKey("action")) {
                    String action = dataMap.get("action");
                    if (action.equals("incoming-call") || action.equals("call-accepted") ||
                            action.equals("call-rejected") || action.equals("call-ended") ||
                            action.equals("offer") || action.equals("answer") ||
                            action.equals("ice-candidate")) {

                        // Déléguer au chatView pour traitement de l'appel
                        Platform.runLater(() -> {
                            if (chatView != null) {
                                chatView.handleCallEvent(dataMap);
                            }
                        });
                        return;
                    }
                }
            }

            // Traiter les autres types de messages comme avant
            if (response.getMessage() != null && response.getMessage().contains("message reçu")) {
                handleNewMessage(response);
            } else if (response.getMessage() != null && response.getMessage().contains("fichier")) {
                handleNewFile(response);
            } else if (response.getData() instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) response.getData();
                if (dataMap.containsKey("content") &&
                        (dataMap.containsKey("from") || dataMap.containsKey("sender"))) {
                    handleGenericMessageData(dataMap);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de la réponse: " + e.getMessage());
        }
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
                }
            } else {
                // Tenter de convertir directement l'objet en Message
                try {
                    message = gson.fromJson(gson.toJson(data), Message.class);
                } catch (Exception e) {
                    System.err.println("Impossible de convertir en Message: " + e.getMessage());
                }
            }

            if (message != null) {
                // Ajouter le message à l'interface utilisateur
                final Message finalMessage = message;
                Platform.runLater(() -> {
                    if (chatView != null) {
                        chatView.addMessageToConversation(finalMessage);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du nouveau message: " + e.getMessage());
        }
    }

    /**
     * Traite des données de message génériques reçues
     */
    private void handleGenericMessageData(Map<String, Object> messageData) {
        try {
            String sender = null;
            if (messageData.containsKey("from")) {
                sender = (String) messageData.get("from");
            } else if (messageData.containsKey("sender")) {
                sender = (String) messageData.get("sender");
            }

            String content = (String) messageData.get("content");
            String receiver = currentUsername;

            if (sender != null && content != null) {
                Message message = new Message();
                message.setSender(sender);
                message.setReceiver(receiver);
                message.setContent(content);
                message.setTimestamp(LocalDateTime.now());
                message.setRead(false);

                // Ajouter le message à l'interface utilisateur
                Platform.runLater(() -> {
                    if (chatView != null) {
                        chatView.addMessageToConversation(message);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement des données de message génériques: " + e.getMessage());
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
            String base64Content = fileData.get("content");

            if (sender == null || filename == null || base64Content == null) {
                System.err.println("Données de fichier incomplètes reçues");
                return;
            }

            // Décoder le contenu Base64
            byte[] fileBytes;
            try {
                fileBytes = Base64.getDecoder().decode(base64Content);
            } catch (IllegalArgumentException e) {
                System.err.println("Erreur lors du décodage Base64 du fichier: " + e.getMessage());
                return;
            }

            // Créer un nom de fichier unique
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueFilename = timestamp + "_" + filename;

            // Définir le chemin de sauvegarde
            String filesDirectory = "files/" + currentUsername;
            Path filePath = Paths.get(filesDirectory, uniqueFilename);

            // Créer le répertoire si nécessaire
            try {
                Files.createDirectories(filePath.getParent());
                // Sauvegarder le fichier physiquement
                Files.write(filePath, fileBytes);
            } catch (IOException e) {
                System.err.println("Erreur lors de la sauvegarde du fichier: " + e.getMessage());
                return;
            }

            // Créer l'objet FileData
            FileData file = new FileData();
            file.setSender(sender);
            file.setReceiver(currentUsername);
            file.setFilename(filename);
            file.setFilepath(filePath.toString());
            file.setTimestamp(LocalDateTime.now());
            file.setRead(false);

            // Afficher le fichier dans l'interface graphique
            Platform.runLater(() -> {
                try {
                    if (chatView != null) {
                        chatView.addFileToConversation(file);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'ajout du fichier à l'UI: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du nouveau fichier: " + e.getMessage());
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