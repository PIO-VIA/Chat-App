package org.personnal.client.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import org.personnal.client.MainClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.RequestType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class ChatController {
    private final MainClient app;
    private final String currentUsername;
    private final ListView<String> userListView = new ListView<>();
    private final ObservableList<String> userList = FXCollections.observableArrayList();
    private final VBox messageArea = new VBox(10);
    private final TextField messageInput = new TextField();
    private final Button sendButton = new Button("📤");

    // Socket manager pour communiquer avec le serveur
    private ClientSocketManager socketManager;

    // Utilisateur sélectionné pour la conversation
    private String selectedUser = null;

    // Couleurs des messages
    private final String COLOR_MESSAGE_OUT = "#DCF8F8";   // Bulles de message envoyé (bleuté)
    private final String COLOR_MESSAGE_IN = "#FFFFFF";    // Bulles de message reçu (blanc)

    public ChatController(MainClient app, String currentUsername) {
        this.app = app;
        this.currentUsername = currentUsername;
        userListView.setItems(userList);

        // Configuration de la zone de messages
        messageArea.setPadding(new Insets(15));
        messageArea.setSpacing(10);

        // Initialiser la connexion socket
        try {
            socketManager = ClientSocketManager.getInstance();
            // Configurer le listener pour les mises à jour de la liste des utilisateurs
            socketManager.setUsersUpdateListener(this::updateUserList);
            // Démarrer l'écoute des messages du serveur
            socketManager.startListeningThread();

            // Demander immédiatement la liste des utilisateurs connectés
            requestConnectedUsers();
        } catch (IOException e) {
            System.err.println("Erreur de connexion au serveur: " + e.getMessage());
            addSystemMessage("⚠️ Erreur de connexion au serveur");
        }

        // Ajout de messages de bienvenue
        Platform.runLater(() -> {
            addSystemMessage("Bienvenue dans le chat! Vous êtes connecté en tant que " + currentUsername);
        });

        initListeners();
    }

    public void updateUserList(List<String> users) {
        Platform.runLater(() -> {
            // Filtrer pour ne pas afficher l'utilisateur actuel dans la liste
            users.removeIf(user -> user.equals(currentUsername));
            userList.setAll(users);

            // Si la liste était vide avant et qu'elle ne l'est plus maintenant, afficher un message
            if (!users.isEmpty()) {
                addSystemMessage("📋 Liste des utilisateurs mise à jour (" + users.size() + " connecté(s))");
            }
        });
    }

    // Demander au serveur la liste des utilisateurs connectés
    public void requestConnectedUsers() {
        try {
            PeerRequest request = new PeerRequest(RequestType.GET_CONNECTED_USERS, Map.of());
            socketManager.sendRequest(request);
        } catch (IOException e) {
            System.err.println("Erreur lors de la demande des utilisateurs connectés: " + e.getMessage());
            addSystemMessage("⚠️ Impossible de récupérer la liste des utilisateurs");
        }
    }

    private void initListeners() {
        // Configurer l'action d'envoi de message
        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());

        // Configurer la sélection d'un utilisateur dans la liste
        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                selectedUser = newValue;
                addSystemMessage("💬 Conversation avec " + selectedUser);
                // Effacer les anciens messages
                messageArea.getChildren().clear();
                // Ajouter un message d'info
                addSystemMessage("📝 Historique de la conversation avec " + selectedUser);
            }
        });

        app.setOnUsersUpdated(users -> {
            if (users != null && users.length > 0) {
                updateUserList(List.of(users));
            }
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (!text.isEmpty()) {
            if (selectedUser == null) {
                addSystemMessage("⚠️ Veuillez sélectionner un destinataire dans la liste des utilisateurs");
                return;
            }

            try {
                // Préparer la requête pour envoyer le message
                Map<String, String> payload = Map.of(
                        "sender", currentUsername,
                        "receiver", selectedUser,
                        "content", text,
                        "read", "false"
                );

                PeerRequest request = new PeerRequest(RequestType.SEND_MESSAGE, payload);
                socketManager.sendRequest(request);

                // Afficher le message envoyé dans notre interface
                addOutgoingMessage(text);
                messageInput.clear();

            } catch (IOException e) {
                System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
                addSystemMessage("⚠️ Impossible d'envoyer le message");
            }
        }
    }

    // Ajouter un message sortant (envoyé par l'utilisateur actuel)
    public void addOutgoingMessage(String text) {
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_RIGHT);
        messageContainer.setPadding(new Insets(5, 0, 5, 0));

        VBox messageBox = createMessageBubble(text, true);
        messageContainer.getChildren().add(messageBox);

        Platform.runLater(() -> {
            messageArea.getChildren().add(messageContainer);
        });
    }

    // Ajouter un message entrant (reçu d'un autre utilisateur)
    public void addIncomingMessage(String sender, String text) {
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_LEFT);
        messageContainer.setPadding(new Insets(5, 0, 5, 0));

        VBox messageBox = createMessageBubble(text, false);

        // Ajouter le nom de l'expéditeur
        Label senderLabel = new Label(sender);
        senderLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        senderLabel.setTextFill(Color.rgb(0, 128, 255));
        messageBox.getChildren().add(0, senderLabel);

        messageContainer.getChildren().add(messageBox);

        Platform.runLater(() -> {
            messageArea.getChildren().add(messageContainer);
        });
    }

    // Ajouter un message système (notification)
    public void addSystemMessage(String text) {
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER);
        messageContainer.setPadding(new Insets(5, 0, 5, 0));

        Label systemMessage = new Label(text);
        systemMessage.setStyle("-fx-background-color: #E1F5FE;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 5 10;");
        systemMessage.setTextFill(Color.rgb(100, 100, 100));
        systemMessage.setFont(Font.font("System", FontWeight.NORMAL, 12));

        messageContainer.getChildren().add(systemMessage);

        Platform.runLater(() -> {
            messageArea.getChildren().add(messageContainer);
        });
    }

    // Créer une bulle de message avec le style WhatsApp
    private VBox createMessageBubble(String text, boolean outgoing) {
        VBox messageBox = new VBox(2);
        messageBox.setMaxWidth(400);

        String bubbleColor = outgoing ? COLOR_MESSAGE_OUT : COLOR_MESSAGE_IN;
        String alignment = outgoing ? "top-right" : "top-left";

        messageBox.setStyle("-fx-background-color: " + bubbleColor + ";" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 8 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");

        // Le texte du message
        Label messageText = new Label(text);
        messageText.setWrapText(true);
        messageText.setFont(Font.font("System", 14));

        // Heure du message
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label timeLabel = new Label(time);
        timeLabel.setFont(Font.font("System", 10));
        timeLabel.setTextFill(Color.GRAY);
        timeLabel.setTextAlignment(TextAlignment.RIGHT);

        // Status de lecture (pour messages sortants)
        if (outgoing) {
            HBox statusBox = new HBox(3);
            statusBox.setAlignment(Pos.CENTER_RIGHT);

            Label checkmark = new Label("✓✓");
            checkmark.setFont(Font.font("System", 10));
            checkmark.setTextFill(Color.rgb(90, 150, 220));  // Bleu pour lu

            statusBox.getChildren().addAll(timeLabel, checkmark);
            messageBox.getChildren().addAll(messageText, statusBox);
        } else {
            messageBox.getChildren().addAll(messageText, timeLabel);
        }

        return messageBox;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public ListView<String> getUserListView() {
        return userListView;
    }

    public VBox getMessageArea() {
        return messageArea;
    }

    public TextField getMessageInputField() {
        return messageInput;
    }

    public Button getSendButton() {
        return sendButton;
    }

    public void handleFileUpload() {
        if (selectedUser == null) {
            addSystemMessage("⚠️ Veuillez sélectionner un destinataire dans la liste des utilisateurs");
            return;
        }

        // Ouvrir un explorateur de fichiers
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier à envoyer");
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            try {
                System.out.println("📁 Fichier sélectionné : " + file.getAbsolutePath());

                // Lire le fichier et le convertir en Base64
                byte[] fileContent = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(fileContent);
                }

                String base64Content = Base64.getEncoder().encodeToString(fileContent);

                // Préparer la requête pour envoyer le fichier
                Map<String, String> payload = Map.of(
                        "sender", currentUsername,
                        "receiver", selectedUser,
                        "filename", file.getName(),
                        "content", base64Content
                );

                PeerRequest request = new PeerRequest(RequestType.SEND_FILE, payload);
                socketManager.sendRequest(request);

                // Afficher dans notre interface
                addOutgoingMessage("📎 " + file.getName());

            } catch (IOException e) {
                System.err.println("Erreur lors de l'envoi du fichier: " + e.getMessage());
                addSystemMessage("⚠️ Impossible d'envoyer le fichier");
            }
        }
    }
}