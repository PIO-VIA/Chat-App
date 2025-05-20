package org.personnal.client.UI.call;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.personnal.client.call.AudioCallManager;
import org.personnal.client.controller.ChatController;

public class CallWindow {
    private final Stage callStage;
    private final AudioCallManager callManager;
    private final ChatController chatController;

    // Composants UI
    private Label statusLabel;
    private Label callerLabel;
    private Label timerLabel;
    private Button acceptButton;
    private Button rejectButton;
    private Button hangupButton;

    // État
    private boolean isIncoming;
    private String callPartner;
    private long callStartTime;
    private Thread timerThread;

    public CallWindow(AudioCallManager callManager, ChatController chatController, boolean isIncoming, String callPartner) {
        this.callManager = callManager;
        this.chatController = chatController;
        this.isIncoming = isIncoming;
        this.callPartner = callPartner;

        // Créer la fenêtre
        callStage = new Stage();
        callStage.initModality(Modality.APPLICATION_MODAL);
        callStage.initStyle(StageStyle.DECORATED);
        callStage.setTitle("Appel audio");
        callStage.setMinWidth(300);
        callStage.setMinHeight(400);
        callStage.setResizable(false);

        // Configurer l'UI
        BorderPane root = setupUI();

        // Configurer l'écouteur d'événements d'appel
        callManager.setCallEventListener(this::handleCallEvent);

        Scene scene = new Scene(root, 300, 400);
        // Ajouter les styles CSS si nécessaire
        callStage.setScene(scene);

        // Fermer la fenêtre quand l'appel est terminé
        callStage.setOnCloseRequest(e -> {
            if (callManager.getCallStatus() != AudioCallManager.CallStatus.IDLE) {
                callManager.endCall();
            }
            stopTimer();
        });
    }

    /**
     * Configure l'interface utilisateur
     */
    private BorderPane setupUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // En-tête: nom du contact et état
        VBox headerBox = new VBox(10);
        headerBox.setAlignment(Pos.CENTER);

        callerLabel = new Label(callPartner);
        callerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        statusLabel = new Label(isIncoming ? "Appel entrant..." : "Appel en cours...");
        statusLabel.setStyle("-fx-font-size: 16px;");

        // Avatar du contact
        Circle avatarCircle = new Circle(50);
        avatarCircle.setFill(Color.valueOf("#1a6fc7"));
        Label avatarLabel = new Label(callPartner.substring(0, 1).toUpperCase());
        avatarLabel.setStyle("-fx-font-size: 36px; -fx-text-fill: white;");
        StackPane avatarPane = new StackPane(avatarCircle, avatarLabel);

        // Timer pour la durée d'appel
        timerLabel = new Label("00:00");
        timerLabel.setStyle("-fx-font-size: 14px;");
        timerLabel.setVisible(false);

        headerBox.getChildren().addAll(callerLabel, statusLabel, avatarPane, timerLabel);
        root.setTop(headerBox);
        BorderPane.setAlignment(headerBox, Pos.CENTER);
        BorderPane.setMargin(headerBox, new Insets(0, 0, 20, 0));

        // Boutons pour accepter/rejeter/raccrocher
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        acceptButton = new Button("Accepter");
        acceptButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 14px;");
        acceptButton.setPrefSize(100, 40);
        acceptButton.setOnAction(e -> acceptCall());
        acceptButton.setVisible(isIncoming);

        rejectButton = new Button("Rejeter");
        rejectButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 14px;");
        rejectButton.setPrefSize(100, 40);
        rejectButton.setOnAction(e -> rejectCall());
        rejectButton.setVisible(isIncoming);

        hangupButton = new Button("Raccrocher");
        hangupButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 14px;");
        hangupButton.setPrefSize(150, 40);
        hangupButton.setOnAction(e -> endCall());
        hangupButton.setVisible(!isIncoming);

        buttonBox.getChildren().addAll(acceptButton, rejectButton, hangupButton);
        root.setBottom(buttonBox);
        BorderPane.setAlignment(buttonBox, Pos.CENTER);
        BorderPane.setMargin(buttonBox, new Insets(20, 0, 0, 0));

        return root;
    }

    /**
     * Gère les événements d'appel reçus du AudioCallManager
     */
    private void handleCallEvent(AudioCallManager.CallEvent event) {
        Platform.runLater(() -> {
            switch (event) {
                case CALL_ACCEPTED:
                    statusLabel.setText("Appel connecté");
                    // Masquer les boutons d'acceptation/rejet et afficher raccrocher
                    acceptButton.setVisible(false);
                    rejectButton.setVisible(false);
                    hangupButton.setVisible(true);
                    // Démarrer le timer
                    startCallTimer();
                    break;
                case CALL_REJECTED:
                    statusLabel.setText("Appel rejeté");
                    // Fermer la fenêtre après un délai
                    closeAfterDelay(2000);
                    break;
                case CALL_ENDED:
                    statusLabel.setText("Appel terminé");
                    stopTimer();
                    // Fermer la fenêtre après un délai
                    closeAfterDelay(2000);
                    break;
                case CALL_ERROR:
                    statusLabel.setText("Erreur d'appel");
                    // Fermer la fenêtre après un délai
                    closeAfterDelay(2000);
                    break;
                case MEDIA_ESTABLISHED:
                    statusLabel.setText("Appel connecté");
                    break;
            }
        });
    }

    /**
     * Accepte un appel entrant
     */
    private void acceptCall() {
        callManager.acceptCall(callPartner);
        statusLabel.setText("Connexion en cours...");

        // Changer les boutons visibles
        acceptButton.setVisible(false);
        rejectButton.setVisible(false);
        hangupButton.setVisible(true);
    }

    /**
     * Rejette un appel entrant
     */
    private void rejectCall() {
        callManager.rejectCall(callPartner);
        statusLabel.setText("Appel rejeté");

        // Fermer la fenêtre après un délai
        closeAfterDelay(1000);
    }

    /**
     * Termine un appel en cours
     */
    private void endCall() {
        callManager.endCall();
        statusLabel.setText("Appel terminé");
        stopTimer();

        // Fermer la fenêtre après un délai
        closeAfterDelay(1000);
    }

    /**
     * Démarre le chronomètre d'appel
     */
    private void startCallTimer() {
        timerLabel.setVisible(true);
        callStartTime = System.currentTimeMillis();

        // Démarrer un thread pour mettre à jour le timer
        timerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    long elapsedTime = System.currentTimeMillis() - callStartTime;
                    updateTimerLabel(elapsedTime);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        timerThread.setDaemon(true);
        timerThread.start();
    }

    /**
     * Met à jour l'affichage du chronomètre
     */
    private void updateTimerLabel(long elapsedMillis) {
        long seconds = elapsedMillis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        String timeText = String.format("%02d:%02d", minutes, seconds);

        Platform.runLater(() -> timerLabel.setText(timeText));
    }

    /**
     * Arrête le chronomètre
     */
    private void stopTimer() {
        if (timerThread != null) {
            timerThread.interrupt();
            timerThread = null;
        }
    }

    /**
     * Ferme la fenêtre après un délai
     */
    private void closeAfterDelay(long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                Platform.runLater(() -> callStage.close());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Affiche la fenêtre d'appel
     */
    public void show() {
        Platform.runLater(() -> callStage.show());
    }
}