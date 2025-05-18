package org.personnal.client.network;

import com.google.gson.Gson;
import javafx.application.Platform;
import org.personnal.client.UI.ChatView;
import org.personnal.client.model.FileData;
import org.personnal.client.model.Message;
import org.personnal.client.protocol.PeerResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Classe qui écoute les messages entrants du serveur en arrière-plan
 */
public class MessageListener extends Thread {
    private final BufferedReader input;
    private final ChatView chatView;
    private final String currentUsername;
    private volatile boolean running = true;
    private final Gson gson = new Gson();

    // File d'attente pour les messages à traiter
    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    // Indicateur pour le traitement en cours
    private volatile boolean processingMessages = false;

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

                // Traiter la réponse du serveur sans bloquer le thread d'écoute
                processResponseAsync(responseJson);

            } catch (SocketTimeoutException e) {
                // Simplement continuer d'écouter
                continue;
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erreur de lecture, nouvelle tentative dans 2s: " + e.getMessage());
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
     * Traite la réponse JSON de manière asynchrone sans bloquer le thread d'écoute
     */
    private void processResponseAsync(String responseJson) {
        // Créer un thread séparé pour le traitement
        new Thread(() -> {
            try {
                PeerResponse response = gson.fromJson(responseJson, PeerResponse.class);

                // Traiter selon le type de message
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
        }).start();
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
                // Ajouter le message à la file d'attente et le traiter
                addMessageToQueueAndProcess(message);
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

                // Ajouter le message à la file d'attente et le traiter
                addMessageToQueueAndProcess(message);
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
            String content = fileData.get("content");

            FileData file = new FileData();
            file.setSender(sender);
            file.setReceiver(currentUsername);
            file.setFilename(filename);
            file.setTimestamp(LocalDateTime.now());
            file.setRead(false);

            // Afficher le fichier dans l'interface graphique
            Platform.runLater(() -> {
                try {
                    chatView.addFileToConversation(file);
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'ajout du fichier à l'UI: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du nouveau fichier: " + e.getMessage());
        }
    }

    /**
     * Ajoute un message à la file d'attente et lance le traitement si nécessaire
     */
    private void addMessageToQueueAndProcess(Message message) {
        messageQueue.add(message);

        // Lancer le traitement s'il n'est pas déjà en cours
        if (!processingMessages) {
            processMessageQueue();
        }
    }

    /**
     * Traite les messages dans la file d'attente un par un
     */
    private void processMessageQueue() {
        processingMessages = true;

        // Traiter les messages sur le thread JavaFX
        Platform.runLater(() -> {
            try {
                Message message;
                // Traiter jusqu'à 10 messages maximum à la fois pour ne pas bloquer l'UI
                int count = 0;
                while ((message = messageQueue.poll()) != null && count < 10) {
                    chatView.addMessageToConversation(message);
                    count++;
                }

                // S'il reste des messages, planifier le traitement du reste
                if (!messageQueue.isEmpty()) {
                    Platform.runLater(this::processMessageQueue);
                } else {
                    processingMessages = false;
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du traitement de la file d'attente de messages: " + e.getMessage());
                processingMessages = false;
            }
        });
    }

    /**
     * Arrête l'écoute des messages
     */
    public void stopListening() {
        running = false;
        interrupt();
    }
}