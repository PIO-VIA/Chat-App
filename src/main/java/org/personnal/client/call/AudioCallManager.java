package org.personnal.client.call;

import org.personnal.client.controller.ChatController;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;
import org.personnal.client.network.ClientSocketManager;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Gestionnaire d'appels audio pour l'interface graphique
 * Coordonne les appels audio entre utilisateurs
 */
public class AudioCallManager {
    // Configuration audio
    private static final int BUFFER_SIZE = 1024;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(44100.0f, 16, 1, true, false);

    // État
    private final ChatController controller;
    private final ClientSocketManager socketManager;
    private String callPartner;
    private CallStatus callStatus = CallStatus.IDLE;
    private final AtomicBoolean audioTransmissionActive = new AtomicBoolean(false);

    // Écouteur d'événements d'appel
    private Consumer<CallEvent> callEventListener;

    // Composants audio
    private TargetDataLine micLine;
    private SourceDataLine speakerLine;
    private ExecutorService audioThreadPool;

    /**
     * Événements d'appel
     */
    public enum CallEvent {
        CALL_ACCEPTED,
        CALL_REJECTED,
        CALL_ENDED,
        CALL_ERROR,
        MEDIA_ESTABLISHED
    }

    /**
     * Statuts d'appel
     */
    public enum CallStatus {
        IDLE,
        CALLING,
        RINGING,
        CONNECTED,
        ERROR
    }

    /**
     * Constructeur
     * @param controller Le contrôleur principal de l'application
     * @throws IOException En cas d'erreur d'initialisation
     */
    public AudioCallManager(ChatController controller) throws IOException {
        this.controller = controller;
        this.socketManager = ClientSocketManager.getInstance();
        initializeAudio();
    }

    /**
     * Initialise les composants audio
     */
    private void initializeAudio() {
        try {
            // Vérifier si le format audio est supporté
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);

            if (!AudioSystem.isLineSupported(micInfo) || !AudioSystem.isLineSupported(speakerInfo)) {
                System.err.println("❌ Format audio non supporté par votre système");
                return;
            }

            // Créer le pool de threads pour l'audio
            audioThreadPool = Executors.newFixedThreadPool(2);
        } catch (Exception e) {
            System.err.println("❌ Erreur d'initialisation audio: " + e.getMessage());
        }
    }

    /**
     * Ouvre les lignes audio pour un appel
     */
    private boolean openAudioLines() {
        try {
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);

            micLine = (TargetDataLine) AudioSystem.getLine(micInfo);
            speakerLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);

            micLine.open(AUDIO_FORMAT);
            speakerLine.open(AUDIO_FORMAT);

            micLine.start();
            speakerLine.start();

            return true;
        } catch (LineUnavailableException e) {
            System.err.println("❌ Erreur lors de l'ouverture des lignes audio: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ferme les lignes audio après un appel
     */
    private void closeAudioLines() {
        if (micLine != null) {
            micLine.stop();
            micLine.close();
            micLine = null;
        }

        if (speakerLine != null) {
            speakerLine.drain();
            speakerLine.stop();
            speakerLine.close();
            speakerLine = null;
        }
    }

    /**
     * Démarre la capture et l'envoi audio
     */
    private void startAudioTransmission() {
        if (callStatus != CallStatus.CONNECTED || micLine == null || speakerLine == null) {
            return;
        }

        audioTransmissionActive.set(true);

        // Thread pour capturer et envoyer l'audio
        audioThreadPool.submit(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (audioTransmissionActive.get() && micLine != null && micLine.isOpen()) {
                try {
                    int bytesRead = micLine.read(buffer, 0, buffer.length);

                    if (bytesRead > 0) {
                        // Encoder les données audio en base64 et les envoyer
                        String audioData = java.util.Base64.getEncoder().encodeToString(buffer);
                        sendCallSignal("audio-data", audioData);
                    }
                } catch (Exception e) {
                    if (audioTransmissionActive.get()) {
                        System.err.println("❌ Erreur lors de la capture audio: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Traite les données audio reçues
     * @param audioData Les données audio encodées en base64
     */
    private void processAudioData(String audioData) {
        if (callStatus != CallStatus.CONNECTED || speakerLine == null || !speakerLine.isOpen()) {
            return;
        }

        try {
            // Décoder et jouer l'audio
            byte[] decodedData = java.util.Base64.getDecoder().decode(audioData);
            speakerLine.write(decodedData, 0, decodedData.length);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement des données audio: " + e.getMessage());
        }
    }

    /**
     * Initie un appel vers un contact
     * @param callee Le destinataire de l'appel
     * @return true si l'appel a été initié, false sinon
     */
    public boolean initiateCall(String callee) {
        if (callStatus != CallStatus.IDLE) {
            System.out.println("❌ Un appel est déjà en cours");
            return false;
        }

        callPartner = callee;
        callStatus = CallStatus.CALLING;

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", controller.getCurrentUsername());
            payload.put("callee", callee);
            payload.put("action", "initiate");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            socketManager.sendRequest(request);

            System.out.println("📞 Appel vers " + callee + " en cours...");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'initiation de l'appel: " + e.getMessage());
            callPartner = null;
            callStatus = CallStatus.IDLE;
            return false;
        }
    }

    /**
     * Accepte un appel entrant
     * @param caller L'initiateur de l'appel
     * @return true si l'appel a été accepté, false sinon
     */
    public boolean acceptCall(String caller) {
        if (callStatus != CallStatus.RINGING || !caller.equals(callPartner)) {
            System.out.println("❌ Impossible d'accepter cet appel");
            return false;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", caller);
            payload.put("callee", controller.getCurrentUsername());
            payload.put("action", "accept");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            socketManager.sendRequest(request);

            // Ouvrir les lignes audio
            if (openAudioLines()) {
                callStatus = CallStatus.CONNECTED;
                notifyCallEvent(CallEvent.CALL_ACCEPTED);

                // Démarrer la transmission audio
                startAudioTransmission();

                return true;
            } else {
                callStatus = CallStatus.ERROR;
                notifyCallEvent(CallEvent.CALL_ERROR);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'acceptation de l'appel: " + e.getMessage());
            callStatus = CallStatus.ERROR;
            notifyCallEvent(CallEvent.CALL_ERROR);
            return false;
        }
    }

    /**
     * Rejette un appel entrant
     * @param caller L'initiateur de l'appel
     * @return true si l'appel a été rejeté, false sinon
     */
    public boolean rejectCall(String caller) {
        if (callStatus != CallStatus.RINGING || !caller.equals(callPartner)) {
            System.out.println("❌ Impossible de rejeter cet appel");
            return false;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", caller);
            payload.put("callee", controller.getCurrentUsername());
            payload.put("action", "reject");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            socketManager.sendRequest(request);

            System.out.println("❌ Appel rejeté");
            callStatus = CallStatus.IDLE;
            callPartner = null;
            notifyCallEvent(CallEvent.CALL_REJECTED);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du rejet de l'appel: " + e.getMessage());
            callStatus = CallStatus.ERROR;
            notifyCallEvent(CallEvent.CALL_ERROR);
            return false;
        }
    }

    /**
     * Termine un appel en cours
     * @return true si l'appel a été terminé, false sinon
     */
    public boolean endCall() {
        if (callStatus == CallStatus.IDLE || callPartner == null) {
            System.out.println("❌ Pas d'appel en cours");
            return false;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", controller.getCurrentUsername());
            payload.put("callee", callPartner);
            payload.put("action", "hangup");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            socketManager.sendRequest(request);

            System.out.println("🛑 Appel terminé");
            closeCallSession();
            notifyCallEvent(CallEvent.CALL_ENDED);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la terminaison de l'appel: " + e.getMessage());
            closeCallSession();
            notifyCallEvent(CallEvent.CALL_ERROR);
            return false;
        }
    }

    /**
     * Ferme la session d'appel
     */
    private void closeCallSession() {
        audioTransmissionActive.set(false);
        callStatus = CallStatus.IDLE;
        callPartner = null;
        closeAudioLines();
    }

    /**
     * Envoie un signal d'appel (SDP, ICE, etc.)
     * @param signalType Le type de signal
     * @param data Les données du signal
     */
    private void sendCallSignal(String signalType, String data) {
        if (callPartner == null) {
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", controller.getCurrentUsername());
            payload.put("callee", callPartner);
            payload.put("action", signalType);
            payload.put("data", data);

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            socketManager.sendRequest(request);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi du signal: " + e.getMessage());
        }
    }

    /**
     * Gère les événements d'appel reçus du serveur
     * @param eventData Les données de l'événement
     */
    public void handleCallEvent(Map<String, String> eventData) {
        String action = eventData.get("action");
        if (action == null) return;

        switch (action) {
            case "incoming-call":
                String caller = eventData.get("caller");
                callPartner = caller;
                callStatus = CallStatus.RINGING;
                System.out.println("\n📞 Appel entrant de " + caller);
                break;

            case "call-accepted":
                callStatus = CallStatus.CONNECTED;

                // Ouvrir les lignes audio
                if (openAudioLines()) {
                    // Démarrer la transmission audio
                    startAudioTransmission();
                    notifyCallEvent(CallEvent.CALL_ACCEPTED);

                    // Simuler l'envoi d'une offre SDP
                    String fakeSdp = "{\"type\":\"offer\",\"sdp\":\"v=0\\r\\no=- 12345 2 IN IP4 127.0.0.1\\r\\ns=-\\r\\nt=0 0\\r\\na=group:BUNDLE audio\\r\\n\"}";
                    sendCallSignal("offer", fakeSdp);
                } else {
                    callStatus = CallStatus.ERROR;
                    notifyCallEvent(CallEvent.CALL_ERROR);
                }
                break;

            case "call-rejected":
                System.out.println("\n❌ Appel rejeté par l'utilisateur");
                closeCallSession();
                notifyCallEvent(CallEvent.CALL_REJECTED);
                break;

            case "call-ended":
                System.out.println("\n🛑 L'appel a été terminé par l'autre utilisateur");
                closeCallSession();
                notifyCallEvent(CallEvent.CALL_ENDED);
                break;

            case "audio-data":
                // Traiter les données audio
                processAudioData(eventData.get("data"));
                break;

            case "offer":
            case "answer":
            case "ice-candidate":
                // Traiter les signaux WebRTC
                System.out.println("📡 Signal " + action + " reçu");
                break;
        }
    }

    /**
     * Définit l'écouteur d'événements d'appel
     * @param listener L'écouteur à définir
     */
    public void setCallEventListener(Consumer<CallEvent> listener) {
        this.callEventListener = listener;
    }

    /**
     * Notifie l'écouteur d'un événement d'appel
     * @param event L'événement à notifier
     */
    private void notifyCallEvent(CallEvent event) {
        if (callEventListener != null) {
            callEventListener.accept(event);
        }
    }

    /**
     * Retourne le statut actuel de l'appel
     * @return Le statut de l'appel
     */
    public CallStatus getCallStatus() {
        return callStatus;
    }

    /**
     * Retourne le partenaire d'appel actuel
     * @return Le partenaire d'appel
     */
    public String getCallPartner() {
        return callPartner;
    }
}