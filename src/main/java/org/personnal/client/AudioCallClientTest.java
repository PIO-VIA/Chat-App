package org.personnal.client;

import com.google.gson.Gson;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Classe de test pour simuler un appel audio entre deux clients
 */
public class AudioCallClientTest {
    // Configuration du serveur
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000; // Adaptez au port de votre serveur

    // Configuration audio
    private static final int BUFFER_SIZE = 1024;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(44100.0f, 16, 1, true, false);

    // État
    private String username;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final Gson gson = new Gson();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean inCall = new AtomicBoolean(false);
    private String callPartner;

    // Composants audio
    private TargetDataLine micLine;
    private SourceDataLine speakerLine;
    private ExecutorService audioThreadPool;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java AudioCallClientTest <username> [<password>]");
            return;
        }

        String username = args[0];
        String password = args.length > 1 ? args[1] : "password123";

        AudioCallClientTest client = new AudioCallClientTest();
        client.username = username;

        if (client.connect()) {
            System.out.println("✅ Connecté au serveur");

            // Connexion avec le compte utilisateur
            if (client.login(username, password)) {
                System.out.println("✅ Connecté en tant que: " + username);

                // Démarrer un thread pour écouter les réponses du serveur
                client.startResponseListener();

                // Afficher le menu interactif
                client.showMenu();
            } else {
                System.out.println("❌ Échec de connexion");
                client.disconnect();
            }
        } else {
            System.out.println("❌ Impossible de se connecter au serveur");
        }
    }

    /**
     * Se connecte au serveur
     */
    private boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connected.set(true);

            // Initialiser les composants audio
            initializeAudio();

            return true;
        } catch (IOException e) {
            System.err.println("❌ Erreur de connexion: " + e.getMessage());
            return false;
        }
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

            // Les lignes audio seront ouvertes uniquement lors d'un appel
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
        if (!inCall.get() || micLine == null || speakerLine == null) {
            return;
        }

        // Thread pour capturer et envoyer l'audio
        audioThreadPool.submit(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (inCall.get() && micLine != null && micLine.isOpen()) {
                try {
                    int bytesRead = micLine.read(buffer, 0, buffer.length);

                    if (bytesRead > 0) {
                        // Encoder les données audio en base64 et les envoyer
                        String audioData = java.util.Base64.getEncoder().encodeToString(buffer);
                        sendCallSignal("audio-data", audioData);
                    }
                } catch (Exception e) {
                    if (inCall.get()) {
                        System.err.println("❌ Erreur lors de la capture audio: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Traite les données audio reçues
     */
    private void processAudioData(String audioData) {
        if (!inCall.get() || speakerLine == null || !speakerLine.isOpen()) {
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
     * Se déconnecte du serveur
     */
    private void disconnect() {
        // Arrêter l'appel en cours si nécessaire
        if (inCall.get()) {
            endCall();
        }

        // Fermer les ressources audio
        closeAudioLines();
        if (audioThreadPool != null) {
            audioThreadPool.shutdown();
        }

        // Envoyer une requête de déconnexion
        if (connected.get()) {
            try {
                Map<String, String> payload = new HashMap<>();
                PeerRequest request = new PeerRequest(RequestType.DISCONNECT, payload);
                sendRequest(request);
            } catch (Exception e) {
                // Ignorer les erreurs lors de la déconnexion
            }
        }

        // Fermer la socket
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            connected.set(false);
            System.out.println("👋 Déconnecté du serveur");
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    /**
     * Se connecte avec un compte utilisateur
     */
    private boolean login(String username, String password) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("password", password);

            PeerRequest request = new PeerRequest(RequestType.LOGIN, payload);
            PeerResponse response = sendRequestAndWaitResponse(request);

            return response != null && response.isSuccess();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la connexion: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envoie une requête au serveur
     */
    private void sendRequest(PeerRequest request) {
        try {
            String jsonRequest = gson.toJson(request);
            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("❌ Erreur d'envoi: " + e.getMessage());
            connected.set(false);
        }
    }

    /**
     * Envoie une requête et attend la réponse
     */
    private PeerResponse sendRequestAndWaitResponse(PeerRequest request) {
        try {
            String jsonRequest = gson.toJson(request);
            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();

            String jsonResponse = reader.readLine();
            return gson.fromJson(jsonResponse, PeerResponse.class);
        } catch (IOException e) {
            System.err.println("❌ Erreur d'envoi/réception: " + e.getMessage());
            connected.set(false);
            return null;
        }
    }

    /**
     * Démarre un thread pour écouter les réponses du serveur
     */
    private void startResponseListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (connected.get()) {
                    String jsonResponse = reader.readLine();
                    if (jsonResponse == null) {
                        System.out.println("⚠️ Connexion fermée par le serveur");
                        connected.set(false);
                        break;
                    }

                    PeerResponse response = gson.fromJson(jsonResponse, PeerResponse.class);
                    handleServerResponse(response);
                }
            } catch (IOException e) {
                if (connected.get()) {
                    System.err.println("❌ Erreur de lecture: " + e.getMessage());
                    connected.set(false);
                }
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Traite les réponses du serveur
     */
    private void handleServerResponse(PeerResponse response) {
        if (response.getData() instanceof Map) {
            Map<String, String> data = (Map<String, String>) response.getData();
            String action = data.get("action");

            if (action != null) {
                switch (action) {
                    case "incoming-call":
                        handleIncomingCall(data);
                        break;
                    case "call-accepted":
                        handleCallAccepted(data);
                        break;
                    case "call-rejected":
                        handleCallRejected(data);
                        break;
                    case "call-ended":
                        handleCallEnded(data);
                        break;
                    case "offer":
                    case "answer":
                    case "ice-candidate":
                        // Traiter les signaux WebRTC
                        System.out.println("📡 Signal " + action + " reçu");
                        break;
                    case "audio-data":
                        // Traiter les données audio
                        processAudioData(data.get("data"));
                        break;
                }
            }
        } else {
            // Afficher les autres types de messages
            System.out.println("\nRéponse du serveur: " + response.getMessage());
        }
    }

    /**
     * Gère un appel entrant
     */
    private void handleIncomingCall(Map<String, String> data) {
        String caller = data.get("caller");
        System.out.println("\n📞 Appel entrant de " + caller);
        System.out.println("Tapez 'accept " + caller + "' pour accepter ou 'reject " + caller + "' pour rejeter");

        callPartner = caller;
        // Ne pas mettre inCall à true maintenant, attendre l'acceptation
    }

    /**
     * Gère l'acceptation d'un appel
     */
    private void handleCallAccepted(Map<String, String> data) {
        System.out.println("\n✅ Appel accepté! Connexion en cours...");

        // Ouvrir les lignes audio
        if (openAudioLines()) {
            inCall.set(true);

            // Démarrer la transmission audio
            startAudioTransmission();

            // Simuler l'envoi d'une offre SDP
            String fakeSdp = "{\"type\":\"offer\",\"sdp\":\"v=0\\r\\no=- 12345 2 IN IP4 127.0.0.1\\r\\ns=-\\r\\nt=0 0\\r\\na=group:BUNDLE audio\\r\\n\"}";
            sendCallSignal("offer", fakeSdp);
        }
    }

    /**
     * Gère le rejet d'un appel
     */
    private void handleCallRejected(Map<String, String> data) {
        System.out.println("\n❌ Appel rejeté par l'utilisateur");
        closeCallSession();
    }

    /**
     * Gère la fin d'un appel
     */
    private void handleCallEnded(Map<String, String> data) {
        System.out.println("\n🛑 L'appel a été terminé par l'autre utilisateur");
        closeCallSession();
    }

    /**
     * Ferme la session d'appel
     */
    private void closeCallSession() {
        inCall.set(false);
        callPartner = null;
        closeAudioLines();
    }

    /**
     * Initie un appel vers un contact
     */
    private void initiateCall(String callee) {
        if (inCall.get()) {
            System.out.println("❌ Vous êtes déjà en appel");
            return;
        }

        callPartner = callee;

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", username);
            payload.put("callee", callee);
            payload.put("action", "initiate");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            sendRequest(request);

            System.out.println("📞 Appel vers " + callee + " en cours...");

            // L'appel sera considéré actif seulement après acceptation
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'initiation de l'appel: " + e.getMessage());
            callPartner = null;
        }
    }

    /**
     * Accepte un appel entrant
     */
    private void acceptCall(String caller) {
        if (inCall.get()) {
            System.out.println("❌ Vous êtes déjà en appel");
            return;
        }

        if (callPartner == null || !callPartner.equals(caller)) {
            System.out.println("❌ Pas d'appel entrant de cet utilisateur");
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", caller);
            payload.put("callee", username);
            payload.put("action", "accept");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            sendRequest(request);

            System.out.println("✅ Appel accepté");

            // Ouvrir les lignes audio
            if (openAudioLines()) {
                inCall.set(true);

                // Démarrer la transmission audio
                startAudioTransmission();
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'acceptation de l'appel: " + e.getMessage());
            callPartner = null;
        }
    }

    /**
     * Rejette un appel entrant
     */
    private void rejectCall(String caller) {
        if (callPartner == null || !callPartner.equals(caller)) {
            System.out.println("❌ Pas d'appel entrant de cet utilisateur");
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", caller);
            payload.put("callee", username);
            payload.put("action", "reject");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            sendRequest(request);

            System.out.println("❌ Appel rejeté");
            callPartner = null;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du rejet de l'appel: " + e.getMessage());
            callPartner = null;
        }
    }

    /**
     * Termine un appel en cours
     */
    private void endCall() {
        if (!inCall.get() || callPartner == null) {
            System.out.println("❌ Pas d'appel en cours");
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", username);
            payload.put("callee", callPartner);
            payload.put("action", "hangup");

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            sendRequest(request);

            System.out.println("🛑 Appel terminé");
            closeCallSession();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la terminaison de l'appel: " + e.getMessage());
            closeCallSession();
        }
    }

    /**
     * Envoie un signal d'appel (SDP, ICE, etc.)
     */
    private void sendCallSignal(String signalType, String data) {
        if (callPartner == null) {
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", username);
            payload.put("callee", callPartner);
            payload.put("action", signalType);
            payload.put("data", data);

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            sendRequest(request);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi du signal: " + e.getMessage());
        }
    }

    /**
     * Affiche le menu interactif
     */
    private void showMenu() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== Client d'Appel Audio ===");
        System.out.println("Commandes disponibles :");
        System.out.println("call <username> - Appeler un utilisateur");
        System.out.println("accept <username> - Accepter un appel entrant");
        System.out.println("reject <username> - Rejeter un appel entrant");
        System.out.println("hangup - Terminer un appel en cours");
        System.out.println("exit - Quitter le programme");

        boolean running = true;
        while (running && connected.get()) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();

            if (input.equals("exit")) {
                running = false;
                disconnect();
            } else if (input.startsWith("call ")) {
                String callee = input.substring(5).trim();
                initiateCall(callee);
            } else if (input.startsWith("accept ")) {
                String caller = input.substring(7).trim();
                acceptCall(caller);
            } else if (input.startsWith("reject ")) {
                String caller = input.substring(7).trim();
                rejectCall(caller);
            } else if (input.equals("hangup")) {
                endCall();
            } else {
                System.out.println("❌ Commande non reconnue");
            }
        }

        scanner.close();
    }
}