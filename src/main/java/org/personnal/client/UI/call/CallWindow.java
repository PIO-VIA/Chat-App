package org.personnal.client.UI.call;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.personnal.client.call.AudioCallManager;
import org.personnal.client.controller.ChatController;

import java.util.Random;

/**
 * Fenêtre d'appel audio améliorée avec contrôles supplémentaires
 */
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
    private ToggleButton muteButton;
    private ToggleButton speakerButton;
    private ProgressBar audioLevelIndicator;

    // État
    private boolean isIncoming;
    private String callPartner;
    private long callStartTime;
    private Thread timerThread;
    private Timeline audioLevelTimeline;
    private boolean isMuted = false;
    private boolean isSpeakerOn = true;
    private final Random random = new Random(); // Pour simuler les niveaux audio

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
        callStage.setMinWidth(350);
        callStage.setMinHeight(450);
        callStage.setResizable(false);

        // Configurer l'UI
        BorderPane root = setupUI();

        // Configurer l'écouteur d'événements d'appel
        callManager.setCallEventListener(this::handleCallEvent);

        Scene scene = new Scene(root, 350, 450);
        // Ajouter le fichier CSS si nécessaire
        // scene.getStylesheets().add("/styles/call-window.css");
        callStage.setScene(scene);

        // Fermer la fenêtre quand l'appel est terminé
        callStage.setOnCloseRequest(e -> {
            if (callManager.getCallStatus() != AudioCallManager.CallStatus.IDLE) {
                callManager.endCall();
            }
            stopTimer();
            stopAudioLevelSimulation();
        });
    }

    /**
     * Configure l'interface utilisateur
     */
    private BorderPane setupUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.getStyleClass().add("call-window");

        // En-tête: nom du contact et état
        VBox headerBox = new VBox(10);
        headerBox.setAlignment(Pos.CENTER);

        callerLabel = new Label(callPartner);
        callerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        statusLabel = new Label(isIncoming ? "Appel entrant..." : "Appel en cours...");
        statusLabel.setStyle("-fx-font-size: 16px;");

        // Avatar du contact (première lettre du nom)
        Circle avatarCircle = new Circle(50);
        avatarCircle.setFill(Color.valueOf("#1a6fc7"));
        Label avatarLabel = new Label(callPartner.substring(0, 1).toUpperCase());
        avatarLabel.setStyle("-fx-font-size: 36px; -fx-text-fill: white;");
        StackPane avatarPane = new StackPane(avatarCircle, avatarLabel);

        // Timer pour la durée d'appel
        timerLabel = new Label("00:00");
        timerLabel.setStyle("-fx-font-size: 14px;");
        timerLabel.setVisible(false);

        // Indicateur de niveau audio
        audioLevelIndicator = new ProgressBar(0);
        audioLevelIndicator.setPrefWidth(200);
        audioLevelIndicator.setVisible(false);
        audioLevelIndicator.getStyleClass().add("audio-level-indicator");

        headerBox.getChildren().addAll(callerLabel, statusLabel, avatarPane, timerLabel, audioLevelIndicator);
        root.setTop(headerBox);
        BorderPane.setAlignment(headerBox, Pos.CENTER);
        BorderPane.setMargin(headerBox, new Insets(0, 0, 20, 0));

        // Zone centrale avec contrôles audio
        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);

        // Contrôles audio (bouton muet, haut-parleur)
        HBox audioControlsBox = new HBox(20);
        audioControlsBox.setAlignment(Pos.CENTER);
        audioControlsBox.setVisible(false); // Caché au début, visible une fois l'appel connecté

        muteButton = new ToggleButton("🎤");
        muteButton.setTooltip(new Tooltip("Couper le micro"));
        muteButton.setPrefSize(60, 60);
        muteButton.setStyle("-fx-background-radius: 30; -fx-min-width: 60px; -fx-min-height: 60px; -fx-font-size: 20px;");
        muteButton.setOnAction(e -> toggleMute());

        speakerButton = new ToggleButton("🔊");
        speakerButton.setTooltip(new Tooltip("Haut-parleur/Écouteur"));
        speakerButton.setPrefSize(60, 60);
        speakerButton.setStyle("-fx-background-radius: 30; -fx-min-width: 60px; -fx-min-height: 60px; -fx-font-size: 20px;");
        speakerButton.setOnAction(e -> toggleSpeaker());

        audioControlsBox.getChildren().addAll(muteButton, speakerButton);

        // Étiquette pour la qualité de l'appel
        Label callQualityLabel = new Label("Qualité de l'appel: En attente");
        callQualityLabel.setStyle("-fx-font-size: 14px;");

        centerBox.getChildren().addAll(audioControlsBox, callQualityLabel);
        root.setCenter(centerBox);

        // Boutons pour accepter/rejeter/raccrocher
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        acceptButton = new Button("Accepter");
        acceptButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 14px;");
        acceptButton.setPrefSize(130, 50);
        acceptButton.setOnAction(e -> acceptCall());
        acceptButton.setVisible(isIncoming);

        rejectButton = new Button("Rejeter");
        rejectButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 14px;");
        rejectButton.setPrefSize(130, 50);
        rejectButton.setOnAction(e -> rejectCall());
        rejectButton.setVisible(isIncoming);

        hangupButton = new Button("Raccrocher");
        hangupButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 14px;");
        hangupButton.setPrefSize(200, 50);
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

                    // Afficher les contrôles audio
                    showAudioControls(true);

                    // Démarrer le timer
                    startCallTimer();

                    // Démarrer la simulation du niveau audio
                    startAudioLevelSimulation();
                    break;

                case CALL_REJECTED:
                    statusLabel.setText("Appel rejeté");
                    // Fermer la fenêtre après un délai
                    closeAfterDelay(2000);
                    break;

                case CALL_ENDED:
                    statusLabel.setText("Appel terminé");
                    stopTimer();
                    stopAudioLevelSimulation();
                    // Fermer la fenêtre après un délai
                    closeAfterDelay(2000);
                    break;

                case CALL_ERROR:
                    statusLabel.setText("Erreur d'appel");
                    // Fermer la fenêtre après un délai
                    closeAfterDelay(2000);
                    break;

                case MEDIA_ESTABLISHED:
                    statusLabel.setText("Qualité audio: Bonne");
                    break;
            }
        });
    }

    /**
     * Affiche ou masque les contrôles audio
     */
    private void showAudioControls(boolean show) {
        for (int i = 0; i < callStage.getScene().getRoot().getChildrenUnmodifiable().size(); i++) {
            if (callStage.getScene().getRoot().getChildrenUnmodifiable().get(i) instanceof BorderPane) {
                BorderPane root = (BorderPane) callStage.getScene().getRoot().getChildrenUnmodifiable().get(i);
                if (root.getCenter() instanceof VBox) {
                    VBox centerBox = (VBox) root.getCenter();
                    for (int j = 0; j < centerBox.getChildren().size(); j++) {
                        if (centerBox.getChildren().get(j) instanceof HBox) {
                            centerBox.getChildren().get(j).setVisible(show);
                        }
                    }
                }
            }
        }

        // Afficher également l'indicateur de niveau audio
        audioLevelIndicator.setVisible(show);
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
        stopAudioLevelSimulation();

        // Fermer la fenêtre après un délai
        closeAfterDelay(1000);
    }

    /**
     * Active/désactive le microphone
     */
    private void toggleMute() {
        isMuted = muteButton.isSelected();

        if (isMuted) {
            muteButton.setText("🔇");
            muteButton.setTooltip(new Tooltip("Activer le micro"));
            // Logique pour couper le micro
            // callManager.setMicrophoneMuted(true);
        } else {
            muteButton.setText("🎤");
            muteButton.setTooltip(new Tooltip("Couper le micro"));
            // Logique pour activer le micro
            // callManager.setMicrophoneMuted(false);
        }
    }

    /**
     * Bascule entre haut-parleur et écouteur
     */
    private void toggleSpeaker() {
        isSpeakerOn = speakerButton.isSelected();

        if (isSpeakerOn) {
            speakerButton.setText("🔈");
            speakerButton.setTooltip(new Tooltip("Activer le haut-parleur"));
            // Logique pour passer en mode écouteur
            // callManager.setSpeakerMode(false);
        } else {
            speakerButton.setText("🔊");
            speakerButton.setTooltip(new Tooltip("Activer l'écouteur"));
            // Logique pour passer en mode haut-parleur
            // callManager.setSpeakerMode(true);
        }
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
        long hours = minutes / 60;
        minutes = minutes % 60;

        String timeText;
        if (hours > 0) {
            timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeText = String.format("%02d:%02d", minutes, seconds);
        }

        Platform.runLater(() -> timerLabel.setText(timeText));
    }

    /**
     * Démarre la simulation du niveau audio
     * Dans une application réelle, ce serait remplacé par le niveau réel du microphone
     */
    private void startAudioLevelSimulation() {
        if (audioLevelTimeline != null) {
            audioLevelTimeline.stop();
        }

        audioLevelTimeline = new Timeline(
                new KeyFrame(Duration.millis(100), event -> {
                    double level = random.nextDouble() * 0.8 + 0.1; // Valeur entre 0.1 et 0.9
                    audioLevelIndicator.setProgress(level);

                    // Modifier la couleur en fonction du niveau
                    if (level > 0.7) {
                        audioLevelIndicator.setStyle("-fx-accent: #28a745;"); // Vert pour bon niveau
                    } else if (level > 0.3) {
                        audioLevelIndicator.setStyle("-fx-accent: #ffc107;"); // Jaune pour niveau moyen
                    } else {
                        audioLevelIndicator.setStyle("-fx-accent: #dc3545;"); // Rouge pour niveau faible
                    }
                })
        );

        audioLevelTimeline.setCycleCount(Timeline.INDEFINITE);
        audioLevelTimeline.play();
    }

    /**
     * Arrête la simulation du niveau audio
     */
    private void stopAudioLevelSimulation() {
        if (audioLevelTimeline != null) {
            audioLevelTimeline.stop();
            audioLevelTimeline = null;
        }
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