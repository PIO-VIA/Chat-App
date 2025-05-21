package org.personnal.client.network;

import com.google.gson.Gson;
import org.personnal.client.UI.ChatView;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
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

    // Indicateur de l'état de la connexion
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    // Paramètres de connexion
    private String serverHost = "localhost";
    private int serverPort = 5000;

    // Temps de timeout réduit
    private static final int SOCKET_TIMEOUT = 30000; // 10 secondes

    // Constructeur privé pour singleton
    private ClientSocketManager() {
        // On supprime les planificateurs automatiques qui ralentissent le client
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
            this.serverHost = host;
            this.serverPort = port;

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
     * Cette méthode est désormais manuelle et n'est plus appelée automatiquement
     */
    // Amélioration de la méthode isUserOnline
    public boolean isUserOnline(String username) {
        // Vérifier le cache d'abord avec une durée d'expiration plus courte (5 secondes)
        CachedStatus cachedStatus = onlineStatusCache.get(username);
        if (cachedStatus != null && !cachedStatus.isExpired()) {
            return cachedStatus.isOnline();
        }

        // Si pas connecté, retourner false immédiatement
        if (!isConnected.get()) {
            return false;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);

            // Ajouter une propriété avec timestamp pour éviter le cache côté serveur
            payload.put("timestamp", String.valueOf(System.currentTimeMillis()));

            synchronized (sendLock) {
                PeerRequest request = new PeerRequest(RequestType.CHECK_ONLINE, payload);
                sendRequest(request);

                // Réduire le timeout pour cette requête spécifique
                socket.setSoTimeout(2000); // 2 secondes

                PeerResponse response = readResponse();

                // Restaurer le timeout normal
                socket.setSoTimeout(SOCKET_TIMEOUT);

                boolean isOnline = false;
                if (response.isSuccess()) {
                    Map<String, String> data = (Map<String, String>) response.getData();
                    isOnline = "true".equals(data.get("online"));
                }

                // Mettre en cache pour 5 secondes seulement
                onlineStatusCache.put(username, new CachedStatus(isOnline, System.currentTimeMillis() + 5000));
                return isOnline;
            }
        } catch (SocketTimeoutException e) {
            // En cas de timeout, mettre en cache "offline" pour un court moment
            onlineStatusCache.put(username, new CachedStatus(false, System.currentTimeMillis() + 3000));
            System.err.println("Timeout lors de la vérification du statut de " + username);
            return false;
        } catch (IOException e) {
            // En cas d'erreur, utiliser la dernière valeur connue si disponible
            if (cachedStatus != null) {
                return cachedStatus.isOnline();
            }
            return false;
        } finally {
            try {
                // Restaurer le timeout normal
                socket.setSoTimeout(SOCKET_TIMEOUT);
            } catch (Exception ignored) {}
        }
    }

    // Nouvelle méthode optimisée pour vérifier plusieurs utilisateurs en même temps
    public Map<String, Boolean> batchCheckOnlineStatus(List<String> usernames) {
        Map<String, Boolean> results = new HashMap<>();
        if (usernames.isEmpty() || !isConnected.get()) {
            return results;
        }

        // D'abord, recueillir tous les résultats en cache qui sont valides
        List<String> usersToCheck = new ArrayList<>();
        for (String username : usernames) {
            CachedStatus status = onlineStatusCache.get(username);
            if (status != null && !status.isExpired()) {
                results.put(username, status.isOnline());
            } else {
                usersToCheck.add(username);
            }
        }

        if (usersToCheck.isEmpty()) {
            return results;
        }

        // Créer une requête par lot pour tous les utilisateurs restants
        Map<String, String> payload = new HashMap<>();
        payload.put("usernames", String.join(",", usersToCheck));
        payload.put("batch", "true");
        payload.put("timestamp", String.valueOf(System.currentTimeMillis()));

        try {
            synchronized (sendLock) {
                PeerRequest request = new PeerRequest(RequestType.CHECK_ONLINE, payload);
                sendRequest(request);

                // Réduire le timeout mais donner plus de temps pour une requête par lot
                socket.setSoTimeout(5000); // 5 secondes

                PeerResponse response = readResponse();

                if (response.isSuccess() && response.getData() instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) response.getData();

                    for (String username : usersToCheck) {
                        boolean online = false;
                        if (data.containsKey(username)) {
                            online = Boolean.parseBoolean(data.get(username).toString());
                        }

                        results.put(username, online);
                        onlineStatusCache.put(username, new CachedStatus(online, System.currentTimeMillis() + 5000));
                    }
                } else {
                    // Échec de la requête par lot, marquer tous comme hors ligne
                    for (String username : usersToCheck) {
                        results.put(username, false);
                        onlineStatusCache.put(username, new CachedStatus(false, System.currentTimeMillis() + 3000));
                    }
                }
            }
        } catch (Exception e) {
            // En cas d'erreur, marquer tous comme hors ligne
            for (String username : usersToCheck) {
                results.put(username, false);
            }
            System.err.println("Erreur lors de la vérification par lot: " + e.getMessage());
        } finally {
            try {
                // Restaurer le timeout normal
                socket.setSoTimeout(SOCKET_TIMEOUT);
            } catch (Exception ignored) {}
        }

        return results;
    }

    /**
     * Rafraîchit manuellement le statut en ligne de tous les contacts
     * @param contactUsernames Liste des noms d'utilisateur des contacts
     * @return Map des statuts mis à jour
     */
    public Map<String, Boolean> refreshOnlineStatus(Iterable<String> contactUsernames) {
        Map<String, Boolean> results = new HashMap<>();

        for (String username : contactUsernames) {
            boolean online = isUserOnline(username);
            results.put(username, online);
        }

        return results;
    }

    /**
     * Démarre le listener de messages pour recevoir les messages entrants
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