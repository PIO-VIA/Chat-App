package org.personnal.client.call;

import com.google.gson.Gson;
import javafx.application.Platform;
import org.personnal.client.controller.ChatController;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AudioCallManager {
    private final ChatController chatController;
    private final ClientSocketManager socketManager;
    private final Gson gson = new Gson();

    // État de l'appel
    private String currentCallPartner;
    private CallStatus callStatus = CallStatus.IDLE;

    // Écouteurs d'événements
    private Consumer<CallEvent> callEventListener;

    // WebRTC Manager pour gérer les connexions audio
    private WebRTCManager webRTCManager;

    // Énumération des statuts d'appel
    public enum CallStatus {
        IDLE,       // Pas d'appel en cours
        CALLING,    // Appel sortant en cours
        RINGING,    // Appel entrant
        CONNECTED,  // Appel connecté
        ENDED       // Appel terminé
    }

    // Événements d'appel
    public enum CallEvent {
        INCOMING_CALL,      // Appel entrant
        CALL_ACCEPTED,      // Appel accepté
        CALL_REJECTED,      // Appel rejeté
        CALL_ENDED,         // Appel terminé
        CALL_ERROR,         // Erreur d'appel
        MEDIA_ESTABLISHED,   // Flux média établi
        CALLING
    }

    public AudioCallManager(ChatController chatController) throws IOException {
        this.chatController = chatController;
        // Créer un socket dédié pour les appels
        this.callSocket = new CallSocketManager("localhost", 5000, chatController.getCurrentUsername());
        this.callSocket.setResponseListener(this::handleCallEvent);
        this.webRTCManager = new WebRTCManager(this::handleWebRTCSignal);

    }


    /**
     * Définit l'écouteur d'événements d'appel
     */
    public void setCallEventListener(Consumer<CallEvent> listener) {
        this.callEventListener = listener;
    }

    /**
     * Initie un appel vers un contact
     */
    public void initiateCall(String callee) {
        if (callStatus != CallStatus.IDLE) {
            // Déjà en appel
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", chatController.getCurrentUsername());
            payload.put("callee", callee);
            payload.put("action", "initiate");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);

            // Mettre à jour l'état avant d'envoyer
            currentCallPartner = callee;
            callStatus = CallStatus.CALLING;

            // Préparer WebRTC sans attendre la réponse
            webRTCManager.initializeConnection();

            // Envoyer la requête sans attendre de réponse
            socketManager.sendRequest(request);

            // Notification de l'UI que l'appel est en cours
            notifyCallEvent(CallEvent.CALLING);

        } catch (Exception e) {
            e.printStackTrace();
            resetCallState();
            notifyCallEvent(CallEvent.CALL_ERROR);
        }
    }

    /**
     * Accepte un appel entrant
     */
    public void acceptCall(String caller) {
        if (callStatus != CallStatus.RINGING || !caller.equals(currentCallPartner)) {
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", caller);
            payload.put("callee", chatController.getCurrentUsername());
            payload.put("action", "accept");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            CompletableFuture.runAsync(() -> {
                try {
                    socketManager.sendRequest(request);
                    PeerResponse response = socketManager.readResponse();

                    if (response.isSuccess()) {
                        callStatus = CallStatus.CONNECTED;

                        // Initialiser WebRTC et attendre l'offre
                        webRTCManager.initializeConnection();
                    } else {
                        endCall();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    endCall();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            endCall();
        }
    }

    /**
     * Rejette un appel entrant
     */
    public void rejectCall(String caller) {
        if (callStatus != CallStatus.RINGING || !caller.equals(currentCallPartner)) {
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", caller);
            payload.put("callee", chatController.getCurrentUsername());
            payload.put("action", "reject");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            CompletableFuture.runAsync(() -> {
                try {
                    socketManager.sendRequest(request);
                    socketManager.readResponse();
                    resetCallState();
                } catch (IOException e) {
                    e.printStackTrace();
                    resetCallState();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            resetCallState();
        }
    }

    /**
     * Termine un appel en cours
     */
    public void endCall() {
        if (callStatus == CallStatus.IDLE) {
            return;
        }

        try {
            String otherParty = currentCallPartner;

            Map<String, String> payload = new HashMap<>();
            payload.put("caller", chatController.getCurrentUsername());
            payload.put("callee", otherParty);
            payload.put("action", "hangup");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            CompletableFuture.runAsync(() -> {
                try {
                    socketManager.sendRequest(request);
                    socketManager.readResponse();

                    // Fermer la connexion WebRTC
                    webRTCManager.closeConnection();
                    resetCallState();
                } catch (IOException e) {
                    e.printStackTrace();
                    resetCallState();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            resetCallState();
        }
    }

    /**
     * Traite les événements d'appel reçus du serveur
     */
    public void handleCallEvent(Map<String, String> eventData) {
        String action = eventData.get("action");

        if ("incoming-call".equals(action)) {
            String caller = eventData.get("caller");
            currentCallPartner = caller;
            callStatus = CallStatus.RINGING;
            notifyCallEvent(CallEvent.INCOMING_CALL);
        }
        else if ("call-accepted".equals(action)) {
            callStatus = CallStatus.CONNECTED;

            // Créer et envoyer l'offre SDP
            CompletableFuture.runAsync(() -> {
                webRTCManager.createOffer();
            });

            notifyCallEvent(CallEvent.CALL_ACCEPTED);
        }
        else if ("call-rejected".equals(action)) {
            resetCallState();
            notifyCallEvent(CallEvent.CALL_REJECTED);
        }
        else if ("call-ended".equals(action)) {
            // Fermer la connexion WebRTC
            webRTCManager.closeConnection();
            resetCallState();
            notifyCallEvent(CallEvent.CALL_ENDED);
        }
        else if ("offer".equals(action) || "answer".equals(action) || "ice-candidate".equals(action)) {
            // Signal WebRTC
            String data = eventData.get("data");
            webRTCManager.handleReceivedSignal(action, data);
        }
    }

    /**
     * Traite les signaux WebRTC à envoyer au pair distant
     */
    private void handleWebRTCSignal(String type, String data) {
        if (currentCallPartner == null || callStatus == CallStatus.IDLE) {
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", chatController.getCurrentUsername());
            payload.put("callee", currentCallPartner);
            payload.put("action", type);
            payload.put("data", data);

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            CompletableFuture.runAsync(() -> {
                try {
                    socketManager.sendRequest(request);
                    socketManager.readResponse();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Réinitialise l'état de l'appel
     */
    private void resetCallState() {
        currentCallPartner = null;
        callStatus = CallStatus.IDLE;
    }

    /**
     * Notifie les écouteurs d'un événement d'appel
     */
    private void notifyCallEvent(CallEvent event) {
        Platform.runLater(() -> {
            if (callEventListener != null) {
                callEventListener.accept(event);
            }
        });
    }

    public String getCurrentCallPartner() {
        return currentCallPartner;
    }

    public CallStatus getCallStatus() {
        return callStatus;
    }
}