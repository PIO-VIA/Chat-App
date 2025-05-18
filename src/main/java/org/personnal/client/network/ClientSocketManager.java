package org.personnal.client.network;

import com.google.gson.Gson;
import org.personnal.client.UI.ChatView;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientSocketManager {
    private static ClientSocketManager instance;

    private Socket socket;
    private BufferedReader input;
    private BufferedWriter output;
    private final Gson gson = new Gson();
    private MessageListener messageListener;
    private final Object sendLock = new Object(); // Pour synchroniser les envois

    // Cache pour le statut en ligne (optimisation)
    private final Map<String, CachedStatus> onlineStatusCache = new HashMap<>();

    // Planificateur pour le ping et nettoyage du cache
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Indicateur de l'état de la connexion
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    // Temps de timeout réduit
    private static final int SOCKET_TIMEOUT = 10000; // 10 secondes au lieu de 30

    // Constructeur privé pour singleton
    private ClientSocketManager() {
        // Démarrer le planificateur pour nettoyer le cache périodiquement
        scheduler.scheduleAtFixedRate(this::clearExpiredCache, 60, 60, TimeUnit.SECONDS);

        // Démarrer un planificateur pour envoyer périodiquement un ping au serveur
        scheduler.scheduleAtFixedRate(this::pingServer, 30, 30, TimeUnit.SECONDS);
    }

    public static ClientSocketManager getInstance() throws IOException {
        if (instance == null) {
            instance = new ClientSocketManager();
            instance.connect("localhost", 5000);
        }
        return instance;
    }

    private void connect(String host, int port) throws IOException {
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(SOCKET_TIMEOUT);  // Timeout de lecture (10 sec)
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            isConnected.set(true);
            System.out.println("✅ Connecté au serveur");
        } catch (IOException e) {
            isConnected.set(false);
            System.err.println("❌ Erreur de connexion au serveur : " + e.getMessage());
            throw e;
        }
    }

    /**
     * Envoie une requête au serveur de manière thread-safe
     */
    public synchronized void sendRequest(PeerRequest request) throws IOException {
        if (!isConnected.get()) {
            throw new IOException("Non connecté au serveur");
        }

        synchronized (sendLock) {
            try {
                String json = gson.toJson(request);
                output.write(json + "\n");
                output.flush();
                System.out.println("Requête envoyée: " + request.getType());
            } catch (IOException e) {
                isConnected.set(false);
                System.err.println("Erreur lors de l'envoi de la requête: " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Lit une réponse du serveur de manière thread-safe
     */
    public synchronized PeerResponse readResponse() throws IOException {
        if (!isConnected.get()) {
            throw new IOException("Non connecté au serveur");
        }

        try {
            String responseJson = input.readLine();
            if (responseJson == null) {
                isConnected.set(false);
                throw new IOException("Connexion fermée par le serveur");
            }
            return gson.fromJson(responseJson, PeerResponse.class);
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout lors de la lecture de la réponse");
            throw e;
        } catch (IOException e) {
            isConnected.set(false);
            System.err.println("Erreur lors de la lecture de la réponse: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Vérifie si un utilisateur est en ligne (avec cache)
     */
    public boolean isUserOnline(String username) {
        // Vérifier dans le cache d'abord
        CachedStatus cachedStatus = onlineStatusCache.get(username);
        if (cachedStatus != null && !cachedStatus.isExpired()) {
            return cachedStatus.isOnline();
        }

        // Si pas dans le cache ou expiré, faire la requête réseau
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);

            PeerRequest request = new PeerRequest(RequestType.CHECK_ONLINE, payload);
            sendRequest(request);
            PeerResponse response = readResponse();

            boolean isOnline = false;
            if (response.isSuccess()) {
                Map<String, String> data = (Map<String, String>) response.getData();
                isOnline = "true".equals(data.get("online"));
            }

            // Mettre en cache le résultat (valide pour 30 secondes)
            onlineStatusCache.put(username, new CachedStatus(isOnline, System.currentTimeMillis() + 30000));

            return isOnline;
        } catch (IOException e) {
            // En cas d'erreur, utiliser la dernière valeur connue ou supposer hors ligne
            return cachedStatus != null ? cachedStatus.isOnline() : false;
        }
    }

    /**
     * Démarre le listener de messages pour recevoir les messages entrants
     * @param chatView Vue de chat pour afficher les messages
     * @param username Nom d'utilisateur actuel
     */
    public void startMessageListener(ChatView chatView, String username) {
        // Arrêter l'ancien listener s'il existe
        if (messageListener != null && messageListener.isAlive()) {
            messageListener.stopListening();
            try {
                messageListener.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Créer et démarrer un nouveau listener uniquement si connecté
        if (isConnected.get()) {
            messageListener = new MessageListener(input, chatView, username);
            messageListener.start();
            System.out.println("Message listener démarré pour " + username);
        } else {
            System.err.println("Impossible de démarrer le MessageListener: non connecté");
        }
    }

    /**
     * Arrête le listener de messages
     */
    public void stopMessageListener() {
        if (messageListener != null) {
            messageListener.stopListening();
            try {
                messageListener.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            messageListener = null;
            System.out.println("Message listener arrêté");
        }
    }

    /**
     * Vérifie si le listener de messages est actif
     */
    public boolean isMessageListenerRunning() {
        return messageListener != null && messageListener.isAlive();
    }

    /**
     * Ferme la connexion au serveur
     */
    public void closeConnection() {
        try {
            // Arrêter les planificateurs
            scheduler.shutdownNow();

            // Arrêter le listener de messages
            stopMessageListener();

            // Fermer les flux et la socket
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();

            isConnected.set(false);
            System.out.println("🔌 Connexion fermée");
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }

    /**
     * Réinitialise la connexion en cas de problème
     */
    public void reconnect() throws IOException {
        closeConnection();
        connect("localhost", 5000);
    }

    /**
     * Nettoie les statuts en ligne expirés du cache
     */
    private void clearExpiredCache() {
        long now = System.currentTimeMillis();
        onlineStatusCache.entrySet().removeIf(entry -> entry.getValue().getExpiryTime() < now);
    }

    /**
     * Envoie un ping au serveur pour maintenir la connexion active
     */
    private void pingServer() {
        if (!isConnected.get()) {
            try {
                reconnect();
            } catch (IOException e) {
                System.err.println("Échec de la reconnexion pendant le ping: " + e.getMessage());
                return;
            }
        }

        try {
            PeerRequest request = new PeerRequest(RequestType.CHECK_ONLINE, Map.of("username", "ping_test"));
            sendRequest(request);
            readResponse();
            // L'important n'est pas la réponse mais de garder la connexion active
        } catch (IOException e) {
            System.err.println("Ping au serveur a échoué: " + e.getMessage());
            isConnected.set(false);
        }
    }

    /**
     * Classe interne pour stocker le statut en ligne avec une date d'expiration
     */
    private static class CachedStatus {
        private final boolean online;
        private final long expiryTime;

        public CachedStatus(boolean online, long expiryTime) {
            this.online = online;
            this.expiryTime = expiryTime;
        }

        public boolean isOnline() {
            return online;
        }

        public long getExpiryTime() {
            return expiryTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}