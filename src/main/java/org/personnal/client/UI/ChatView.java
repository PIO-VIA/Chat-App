package org.personnal.client.UI;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.personnal.client.controller.ChatController;

public class ChatView {

    private final BorderPane layout;
    private final ChatController controller;

    public ChatView(ChatController controller) {
        this.controller = controller;
        this.layout = new BorderPane();
        initUI();
    }

    private void initUI() {
        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #075E54;");
        Label usernameLabel = new Label("🟢 Connecté en tant que : " + controller.getCurrentUsername());
        usernameLabel.setTextFill(Color.WHITE);
        usernameLabel.setFont(Font.font(16));
        header.getChildren().add(usernameLabel);

        // Liste utilisateurs
        ListView<String> userList = controller.getUserListView();
        userList.setPrefWidth(200);
        userList.setStyle("-fx-background-color: #e0f7fa;");

        // Zone messages
        VBox messageArea = controller.getMessageArea();
        ScrollPane scrollPane = new ScrollPane(messageArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white;");

        // Zone d'entrée
        HBox inputArea = new HBox(10);
        inputArea.setPadding(new Insets(10));
        inputArea.setStyle("-fx-background-color: #f0f0f0;");

        Button attachFileButton = new Button("📎"); // icône trombone
        attachFileButton.setStyle("-fx-font-size: 16; -fx-background-color: transparent;");
        attachFileButton.setOnAction(e -> controller.handleFileUpload());

        TextField messageField = controller.getMessageInputField();
        messageField.setPromptText("Écrire un message...");
        messageField.setPrefWidth(500);

        Button sendButton = controller.getSendButton();
        sendButton.setText("💬 Envoyer");
        sendButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white;");

        inputArea.getChildren().addAll(attachFileButton, messageField, sendButton);

        // Layout principal
        layout.setTop(header);
        layout.setLeft(userList);
        layout.setCenter(scrollPane);
        layout.setBottom(inputArea);
    }

    public BorderPane getView() {
        return layout;
    }
}
