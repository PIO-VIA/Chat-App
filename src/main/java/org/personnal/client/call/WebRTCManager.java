package org.personnal.client.call;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class WebRTCManager {
    // Interface pour les signaux WebRTC à envoyer
    private BiConsumer<String, String> signalSender;

    // État de la connexion
    private boolean isInitialized = false;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    // Composants audio
    private TargetDataLine micLine;
    private SourceDataLine speakerLine;
    private AudioFormat audioFormat;

    // Thread pool pour la gestion des flux audio
    private ExecutorService audioThreadPool;

    // Taille du buffer audio
    private static final int BUFFER_SIZE = 1024;

    public WebRTCManager(BiConsumer<String, String> signalSender) {
        this.signalSender = signalSender;
        this.audioThreadPool = Executors.newFixedThreadPool(2);

        // Format audio: 44.1kHz, 16bit, mono, signé, little-endian
        this.audioFormat = new AudioFormat(44100.0f, 16, 1, true, false);
    }

    /**
     * Initialise la connexion WebRTC
     */
    public void initializeConnection() {
        if (isInitialized) {
            return;
        }

        try {
            // Configurer le microphone
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(micInfo)) {
                System.err.println("Microphone non supporté");
                return;
            }

            // Configurer les haut-parleurs
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(speakerInfo)) {
                System.err.println("Haut-parleurs non supportés");
                return;
            }

            // Obtenir les lignes audio
            micLine = (TargetDataLine) AudioSystem.getLine(micInfo);
            speakerLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);

            isInitialized = true;

        } catch (LineUnavailableException e) {
            System.err.println("Erreur d'initialisation audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crée et envoie une offre SDP
     */
    public void createOffer() {
        if (!isInitialized) {
            initializeConnection();
        }

        // Dans une implémentation WebRTC réelle, nous créerions une offre SDP
        // Pour notre simulation, nous allons simplement envoyer un signal pour démarrer le flux audio
        String fakeOffer = "{\"type\":\"offer\",\"sdp\":\"v=0\\r\\no=- 12345 2 IN IP4 127.0.0.1\\r\\ns=-\\r\\nt=0 0\\r\\na=group:BUNDLE audio\\r\\n\"}";
        signalSender.accept("offer", fakeOffer);

        // Démarrer la capture audio
        startAudioCapture();
    }

    /**
     * Crée et envoie une réponse SDP
     */
    public void createAnswer() {
        if (!isInitialized) {
            return;
        }

        // Dans une implémentation WebRTC réelle, nous créerions une réponse SDP
        // Pour notre simulation, nous allons simplement envoyer un signal pour démarrer le flux audio de notre côté
        String fakeAnswer = "{\"type\":\"answer\",\"sdp\":\"v=0\\r\\no=- 54321 1 IN IP4 127.0.0.1\\r\\ns=-\\r\\nt=0 0\\r\\na=group:BUNDLE audio\\r\\n\"}";
        signalSender.accept("answer", fakeAnswer);

        // Démarrer la capture audio
        startAudioCapture();
    }

    /**
     * Démarre la capture audio depuis le microphone
     */
    private void startAudioCapture() {
        if (isRecording || !isInitialized) {
            return;
        }

        try {
            // Ouvrir et démarrer la ligne du microphone
            micLine.open(audioFormat);
            micLine.start();

            // Marquer comme en cours d'enregistrement
            isRecording = true;

            // Démarrer la lecture
            startAudioPlayback();

            // Démarrer un thread pour capturer et envoyer l'audio
            audioThreadPool.submit(() -> {
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while (isRecording) {
                        // Lire depuis le microphone
                        int bytesRead = micLine.read(buffer, 0, buffer.length);

                        if (bytesRead > 0) {
                            // Dans une vraie implémentation WebRTC, nous encoderions et enverrions ces données
                            // Pour notre simulation, nous pouvons simplement les envoyer en base64
                            String audioData = java.util.Base64.getEncoder().encodeToString(buffer);
                            signalSender.accept("audio-data", audioData);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur de capture audio: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // Nettoyer si le thread se termine
                    if (micLine != null && micLine.isOpen()) {
                        micLine.stop();
                        micLine.close();
                    }
                    isRecording = false;
                }
            });

        } catch (LineUnavailableException e) {
            System.err.println("Erreur lors du démarrage de la capture audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Démarre la lecture audio vers les haut-parleurs
     */
    private void startAudioPlayback() {
        if (isPlaying || !isInitialized) {
            return;
        }

        try {
            // Ouvrir et démarrer la ligne des haut-parleurs
            speakerLine.open(audioFormat);
            speakerLine.start();

            // Marquer comme en cours de lecture
            isPlaying = true;

        } catch (LineUnavailableException e) {
            System.err.println("Erreur lors du démarrage de la lecture audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Traite les données audio reçues
     */
    private void processReceivedAudioData(String base64AudioData) {
        if (!isPlaying || !isInitialized) {
            startAudioPlayback();
        }

        try {
            // Décoder les données audio
            byte[] audioData = java.util.Base64.getDecoder().decode(base64AudioData);

            // Envoyer aux haut-parleurs
            if (speakerLine != null && speakerLine.isOpen()) {
                speakerLine.write(audioData, 0, audioData.length);
            }

        } catch (Exception e) {
            System.err.println("Erreur de traitement audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gère les signaux WebRTC reçus
     */
    public void handleReceivedSignal(String type, String data) {
        if (!isInitialized && !type.equals("audio-data")) {
            initializeConnection();
        }

        switch (type) {
            case "offer":
                // Recevoir une offre et y répondre
                createAnswer();
                break;
            case "answer":
                // Recevoir une réponse et finaliser la connexion
                break;
            case "ice-candidate":
                // Traiter un candidat ICE pour la connexion
                break;
            case "audio-data":
                // Traiter les données audio reçues
                processReceivedAudioData(data);
                break;
        }
    }

    /**
     * Ferme la connexion WebRTC
     */
    public void closeConnection() {
        // Arrêter l'enregistrement
        isRecording = false;

        // Arrêter la lecture
        isPlaying = false;

        // Fermer les ressources audio
        if (micLine != null) {
            micLine.stop();
            micLine.close();
        }

        if (speakerLine != null) {
            speakerLine.drain();
            speakerLine.stop();
            speakerLine.close();
        }

        // Arrêter le thread pool
        audioThreadPool.shutdown();

        isInitialized = false;
    }
}