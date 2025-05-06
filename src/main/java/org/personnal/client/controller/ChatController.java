package org.personnal.client.controller;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.personnal.client.MainClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


import java.io.File;

public class ChatController {
    private final MainClient app;
    private final String currentUsername;
    private final ListView<String> userListView = new ListView<>();
    private final ObservableList<String> userList = FXCollections.observableArrayList();
    private final VBox messageArea = new VBox(10);
    private final TextField messageInput = new TextField();
    private final Button sendButton = new Button("Envoyer");

    public ChatController(MainClient app, String currentUsername) {
        this.app = app;
        this.currentUsername = currentUsername;
        userListView.setItems(userList);
        initListeners();
        app.setOnUsersUpdated(this::updateUserList);
    }
    public void updateUserList(String[] users) {
        Platform.runLater(() -> {
            userList.setAll(users);
        });
    }
    private void initListeners() {
        sendButton.setOnAction(e -> {
            String text = messageInput.getText().trim();
            if (!text.isEmpty()) {
                // TODO: envoyer le message au serveur ici
                messageArea.getChildren().add(new Label("Moi : " + text));
                messageInput.clear();
            }
        });

        app.setOnUsersUpdated(users -> updateUserList(users));

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
        // Ouvrir un explorateur de fichiers
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier à envoyer");
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            System.out.println("📁 Fichier sélectionné : " + file.getAbsolutePath());
            // TODO : envoyer le fichier au serveur
            messageArea.getChildren().add(new Label("📤 Moi (fichier) : " + file.getName()));
        }
    }

}
