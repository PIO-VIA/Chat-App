package org.personnal.client.UI.call;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.personnal.client.call.AudioDeviceManager;
import org.personnal.client.call.AudioDeviceManager.AudioDeviceInfo;

import javax.sound.sampled.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fenêtre de configuration des paramètres audio
 */
public class AudioPreferencesWindow {

    private final Stage stage;
    private ComboBox<AudioDeviceInfo> inputDeviceComboBox;
    private ComboBox<AudioDeviceInfo> outputDeviceComboBox;
    private Slider micVolumeSlider;
    private Slider speakerVolumeSlider;
    private Slider silenceThresholdSlider;
    private CheckBox echoSuppressionCheckBox;
    private CheckBox noiseReductionCheckBox;

    public AudioPreferencesWindow() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Préférences audio");
        stage.setMinWidth(450);
        stage.setMinHeight(400);

        VBox mainLayout = setupUI();
        Scene scene = new Scene(mainLayout);
        stage.setScene(scene);
    }

    /**
     * Configure l'interface utilisateur
     */
    private VBox setupUI() {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        // Titre
        Label titleLabel = new Label("Paramètres audio");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Périphériques audio
        TitledPane devicesPane = createDevicesSection();

        // Volume
        TitledPane volumePane = createVolumeSection();

        // Options avancées
        TitledPane advancedPane = createAdvancedSection();

        // Boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("Enregistrer");
        saveButton.setDefaultButton(true);
        saveButton.setOnAction(e -> savePreferences());

        Button cancelButton = new Button("Annuler");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> stage.close());

        Button testButton = new Button("Tester");
        testButton.setOnAction(e -> testAudioSettings());

        buttonBox.getChildren().addAll(saveButton, testButton, cancelButton);

        // Ajouter tous les éléments
        mainLayout.getChildren().addAll(
                titleLabel,
                devicesPane,
                volumePane,
                advancedPane,
                buttonBox
        );

        return mainLayout;
    }

    /**
     * Crée la section des périphériques audio
     */
    private TitledPane createDevicesSection() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Liste des périphériques d'entrée
        Label inputLabel = new Label("Microphone:");
        grid.add(inputLabel, 0, 0);

        List<AudioDeviceInfo> inputDevices = AudioDeviceManager.getInputDevices();
        inputDeviceComboBox = new ComboBox<>(FXCollections.observableArrayList(inputDevices));
        inputDeviceComboBox.setPrefWidth(300);
        grid.add(inputDeviceComboBox, 1, 0);

        // Liste des périphériques de sortie
        Label outputLabel = new Label("Haut-parleur:");
        grid.add(outputLabel, 0, 1);

        List<AudioDeviceInfo> outputDevices = AudioDeviceManager.getOutputDevices();
        outputDeviceComboBox = new ComboBox<>(FXCollections.observableArrayList(outputDevices));
        outputDeviceComboBox.setPrefWidth(300);
        grid.add(outputDeviceComboBox, 1, 1);

        // Sélectionner les périphériques préférés actuels
        selectCurrentPreferredDevices();

        TitledPane devicePane = new TitledPane("Périphériques audio", grid);
        devicePane.setCollapsible(false);
        return devicePane;
    }

    /**
     * Crée la section de réglage du volume
     */
    private TitledPane createVolumeSection() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Volume du microphone
        Label micLabel = new Label("Volume du microphone:");
        grid.add(micLabel, 0, 0);

        micVolumeSlider = new Slider(0, 1, AudioDeviceManager.getMicrophoneVolume());
        micVolumeSlider.setShowTickLabels(true);
        micVolumeSlider.setShowTickMarks(true);
        micVolumeSlider.setMajorTickUnit(0.25);
        micVolumeSlider.setBlockIncrement(0.1);
        micVolumeSlider.setPrefWidth(300);
        grid.add(micVolumeSlider, 1, 0);

        // Volume du haut-parleur
        Label speakerLabel = new Label("Volume du haut-parleur:");
        grid.add(speakerLabel, 0, 1);

        speakerVolumeSlider = new Slider(0, 1, AudioDeviceManager.getSpeakerVolume());
        speakerVolumeSlider.setShowTickLabels(true);
        speakerVolumeSlider.setShowTickMarks(true);
        speakerVolumeSlider.setMajorTickUnit(0.25);
        speakerVolumeSlider.setBlockIncrement(0.1);
        speakerVolumeSlider.setPrefWidth(300);
        grid.add(speakerVolumeSlider, 1, 1);

        TitledPane volumePane = new TitledPane("Volume", grid);
        volumePane.setCollapsible(false);
        return volumePane;
    }

    /**
     * Crée la section des options avancées
     */
    private TitledPane createAdvancedSection() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Seuil de détection du silence
        Label silenceLabel = new Label("Seuil de silence:");
        grid.add(silenceLabel, 0, 0);

        silenceThresholdSlider = new Slider(0, 0.1, 0.02); // Valeurs typiques entre 0 et 0.1
        silenceThresholdSlider.setShowTickLabels(true);
        silenceThresholdSlider.setShowTickMarks(true);
        silenceThresholdSlider.setMajorTickUnit(0.02);
        silenceThresholdSlider.setBlockIncrement(0.01);
        silenceThresholdSlider.setPrefWidth(300);
        grid.add(silenceThresholdSlider, 1, 0);

        // Suppression d'écho
        echoSuppressionCheckBox = new CheckBox("Suppression d'écho");
        grid.add(echoSuppressionCheckBox, 0, 1, 2, 1);

        // Réduction de bruit
        noiseReductionCheckBox = new CheckBox("Réduction de bruit");
        grid.add(noiseReductionCheckBox, 0, 2, 2, 1);

        TitledPane advancedPane = new TitledPane("Options avancées", grid);
        advancedPane.setCollapsible(true);
        advancedPane.setExpanded(false);
        return advancedPane;
    }

    /**
     * Sélectionne les périphériques préférés actuels dans les combobox
     */
    private void selectCurrentPreferredDevices() {
        // Cette méthode devrait récupérer les périphériques préférés actuels
        // et les sélectionner dans les combobox
        // Pour l'instant, on sélectionne simplement le premier disponible

        if (!inputDeviceComboBox.getItems().isEmpty()) {
            inputDeviceComboBox.getSelectionModel().select(0);
        }

        if (!outputDeviceComboBox.getItems().isEmpty()) {
            outputDeviceComboBox.getSelectionModel().select(0);
        }
    }

    /**
     * Enregistre les préférences
     */
    private void savePreferences() {
        // Enregistrer le périphérique d'entrée préféré
        AudioDeviceInfo selectedInputDevice = inputDeviceComboBox.getValue();
        if (selectedInputDevice != null) {
            AudioDeviceManager.setPreferredInputDevice(selectedInputDevice);
        }

        // Enregistrer le périphérique de sortie préféré
        AudioDeviceInfo selectedOutputDevice = outputDeviceComboBox.getValue();
        if (selectedOutputDevice != null) {
            AudioDeviceManager.setPreferredOutputDevice(selectedOutputDevice);
        }

        // Enregistrer les volumes
        AudioDeviceManager.setMicrophoneVolume((float) micVolumeSlider.getValue());
        AudioDeviceManager.setSpeakerVolume((float) speakerVolumeSlider.getValue());

        // Afficher un message de confirmation
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Préférences audio");
        alert.setHeaderText(null);
        alert.setContentText("Les préférences audio ont été enregistrées avec succès.");
        alert.showAndWait();

        stage.close();
    }

    /**
     * Teste les paramètres audio actuels
     */
    private void testAudioSettings() {
        // Créer une fenêtre de test
        Stage testStage = new Stage();
        testStage.initModality(Modality.APPLICATION_MODAL);
        testStage.setTitle("Test audio");

        VBox testLayout = new VBox(20);
        testLayout.setPadding(new Insets(20));
        testLayout.setAlignment(Pos.CENTER);

        // Indicateur de niveau audio du microphone
        Label micLevelLabel = new Label("Niveau du microphone");
        ProgressBar micLevelIndicator = new ProgressBar(0);
        micLevelIndicator.setPrefWidth(300);
        micLevelIndicator.setStyle("-fx-accent: #1a6fc7;");

        // Label indiquant si le son est détecté
        Label soundDetectedLabel = new Label("Aucun son détecté");
        soundDetectedLabel.setStyle("-fx-text-fill: #dc3545;");

        // Boutons de contrôle
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button startButton = new Button("Démarrer le test");
        Button playTestSoundButton = new Button("Jouer un son test");
        Button closeButton = new Button("Fermer");

        buttonBox.getChildren().addAll(startButton, playTestSoundButton, closeButton);

        // Ajouter les composants à la mise en page
        testLayout.getChildren().addAll(
                new Label("Test des paramètres audio"),
                micLevelLabel,
                micLevelIndicator,
                soundDetectedLabel,
                buttonBox
        );

        Scene testScene = new Scene(testLayout, 400, 300);
        testStage.setScene(testScene);
        testStage.show();

        // Variables pour le test audio
        AtomicBoolean testRunning = new AtomicBoolean(false);
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        final TargetDataLine[] micLine = {null};
        final Timeline[] updateTimeline = {null};

        // Événements des boutons
        startButton.setOnAction(e -> {
            if (testRunning.get()) {
                // Arrêter le test
                testRunning.set(false);
                startButton.setText("Démarrer le test");

                if (updateTimeline[0] != null) {
                    updateTimeline[0].stop();
                }

                if (micLine[0] != null && micLine[0].isOpen()) {
                    micLine[0].stop();
                    micLine[0].close();
                }
            } else {
                // Démarrer le test
                try {
                    // Obtenir le périphérique sélectionné
                    AudioDeviceInfo selectedInputDevice = inputDeviceComboBox.getValue();
                    if (selectedInputDevice != null) {
                        // Obtenir la ligne audio
                        Mixer mixer = AudioSystem.getMixer(selectedInputDevice.getMixerInfo());
                        micLine[0] = (TargetDataLine) mixer.getLine(new Line.Info(TargetDataLine.class));
                        micLine[0].open(format);
                        micLine[0].start();

                        // Démarrer la mise à jour de l'indicateur
                        byte[] buffer = new byte[2048]; // 1024 échantillons
                        float silenceThreshold = (float) silenceThresholdSlider.getValue();

                        updateTimeline[0] = new Timeline(
                                new KeyFrame(Duration.millis(100), event -> {
                                    if (micLine[0] != null && micLine[0].isOpen()) {
                                        int bytesRead = micLine[0].read(buffer, 0, buffer.length);

                                        if (bytesRead > 0) {
                                            // Appliquer le volume
                                            float volume = (float) micVolumeSlider.getValue();
                                            byte[] volumeAdjustedBuffer = AudioDeviceManager.applyVolume(buffer, volume);

                                            // Calculer le niveau audio
                                            double level = calculateAudioLevel(volumeAdjustedBuffer);
                                            micLevelIndicator.setProgress(level);

                                            // Vérifier si ce n'est pas du silence
                                            boolean isSilence = AudioDeviceManager.isSilence(volumeAdjustedBuffer, silenceThreshold);

                                            if (!isSilence) {
                                                soundDetectedLabel.setText("Son détecté !");
                                                soundDetectedLabel.setStyle("-fx-text-fill: #28a745;");
                                            } else {
                                                soundDetectedLabel.setText("Aucun son détecté");
                                                soundDetectedLabel.setStyle("-fx-text-fill: #dc3545;");
                                            }

                                            // Changer la couleur de l'indicateur en fonction du niveau
                                            if (level > 0.7) {
                                                micLevelIndicator.setStyle("-fx-accent: #dc3545;"); // Rouge si trop fort
                                            } else if (level > 0.3) {
                                                micLevelIndicator.setStyle("-fx-accent: #28a745;"); // Vert pour bon niveau
                                            } else {
                                                micLevelIndicator.setStyle("-fx-accent: #ffc107;"); // Jaune pour niveau faible
                                            }
                                        }
                                    }
                                })
                        );
                        updateTimeline[0].setCycleCount(Timeline.INDEFINITE);
                        updateTimeline[0].play();

                        testRunning.set(true);
                        startButton.setText("Arrêter le test");
                    } else {
                        showErrorAlert("Aucun périphérique d'entrée sélectionné.");
                    }
                } catch (Exception ex) {
                    showErrorAlert("Erreur lors du test audio: " + ex.getMessage());
                }
            }
        });

        // Jouer un son test pour vérifier les haut-parleurs
        playTestSoundButton.setOnAction(e -> {
            try {
                // Obtenir le périphérique sélectionné
                AudioDeviceInfo selectedOutputDevice = outputDeviceComboBox.getValue();
                if (selectedOutputDevice != null) {
                    // Créer un thread pour jouer le son
                    new Thread(() -> {
                        try {
                            // Obtenir la ligne audio
                            Mixer mixer = AudioSystem.getMixer(selectedOutputDevice.getMixerInfo());
                            SourceDataLine speakerLine = (SourceDataLine) mixer.getLine(new Line.Info(SourceDataLine.class));
                            speakerLine.open(format);
                            speakerLine.start();

                            // Générer un son de test (une sinusoïde à 440 Hz)
                            float frequency = 440; // La (A4)
                            float sampleRate = format.getSampleRate();
                            byte[] buffer = new byte[4096];

                            // Durée du son test (1 seconde)
                            for (int i = 0; i < sampleRate / (buffer.length / 2); i++) {
                                for (int j = 0; j < buffer.length; j += 2) {
                                    double angle = 2.0 * Math.PI * frequency * j / (2 * sampleRate);
                                    short sample = (short) (Short.MAX_VALUE * Math.sin(angle) * speakerVolumeSlider.getValue());

                                    // PCM 16 bits signé, little-endian
                                    buffer[j] = (byte) (sample & 0xFF);
                                    buffer[j + 1] = (byte) ((sample >> 8) & 0xFF);
                                }

                                speakerLine.write(buffer, 0, buffer.length);
                            }

                            speakerLine.drain();
                            speakerLine.stop();
                            speakerLine.close();
                        } catch (Exception ex) {
                            Platform.runLater(() -> showErrorAlert("Erreur lors de la lecture du son test: " + ex.getMessage()));
                        }
                    }).start();
                } else {
                    showErrorAlert("Aucun périphérique de sortie sélectionné.");
                }
            } catch (Exception ex) {
                showErrorAlert("Erreur lors du test audio: " + ex.getMessage());
            }
        });

        // Fermer la fenêtre de test
        closeButton.setOnAction(e -> {
            if (micLine[0] != null && micLine[0].isOpen()) {
                micLine[0].stop();
                micLine[0].close();
            }

            if (updateTimeline[0] != null) {
                updateTimeline[0].stop();
            }

            testStage.close();
        });

        // Assurer le nettoyage des ressources lors de la fermeture
        testStage.setOnCloseRequest(e -> {
            if (micLine[0] != null && micLine[0].isOpen()) {
                micLine[0].stop();
                micLine[0].close();
            }

            if (updateTimeline[0] != null) {
                updateTimeline[0].stop();
            }
        });
    }

    /**
     * Calcule le niveau audio d'un échantillon audio
     * @param buffer Échantillon audio
     * @return Niveau audio entre 0.0 et 1.0
     */
    private double calculateAudioLevel(byte[] buffer) {
        long sum = 0;

        // Pour un format PCM 16 bits signé little-endian
        for (int i = 0; i < buffer.length; i += 2) {
            if (i + 1 < buffer.length) {
                short sample = (short) ((buffer[i+1] << 8) | (buffer[i] & 0xFF));
                sum += Math.abs(sample);
            }
        }

        // Calculer la moyenne
        double average = sum / (buffer.length / 2.0);

        // Normaliser entre 0 et 1
        return Math.min(1.0, average / (Short.MAX_VALUE * 0.8));
    }

    /**
     * Affiche une alerte d'erreur
     * @param message Message d'erreur
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche la fenêtre des préférences audio
     */
    public void show() {
        stage.showAndWait();
    }
}