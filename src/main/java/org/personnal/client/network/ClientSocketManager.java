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
    private final Object sendLock = new Object();

    // *** CACHE AMÉLIORÉ ***
    private final Map<String, CachedStatus> onlineStatusCache = new ConcurrentHashMap<>();
    private final Map<String, CachedUserExists> userExistsCache = new ConcurrentHashMap<>();

    // Indicateur de l'état de la connexion
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    // Paramètres de connexion
    private String serverHost = "localhost";
    private int serverPort = 5000;
    private static final int SOCKET_TIMEOUT = 30000; // 30 secondes

    // *** DURÉES DE CACHE OPTIMISÉES ***
    private static final long ONLINE_CACHE_DURATION = 60000; // 1 minute pour statut en ligne
    private static final long USER_EXISTS_CACHE_DURATION = 3600000; // 1 heure pour existence utilisateur

    // *** GESTIONNAIRE DE REQUÊTES PAR LOT ***
    private final Map<String, CompletableFuture<Map<String, Boolean>>> pendingBatchRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService batchScheduler = Executors.newSingleThreadScheduledExecutor();

    // Constructeur privé pour singleton
    private ClientSocketManager() {
        // Nettoyer le cache périodiquement
        batchScheduler.scheduleAtFixedRate(this::cleanExpiredCache, 5, 5, TimeUnit.MINUTES);
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
            socket.setSoTimeout(SOCKET_TIMEOUT);
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
     * *** MÉTHODE OPTIMISÉE POUR VÉRIFIER LE STATUT EN LIGNE ***
     * Utilise un cache intelligent et des requêtes par lot
     */
    public boolean isUserOnline(String username) {
        if (!isConnected.get() || username == null || username.trim().isEmpty()) {
            return false;
        }

        // Vérifier le cache d'abord
        CachedStatus cachedStatus = onlineStatusCache.get(username);
        if (cachedStatus != null && !cachedStatus.isExpired()) {
            return cachedStatus.isOnline();
        }

        // Si pas en cache ou expiré, retourner false par défaut et déclencher une mise à jour asynchrone
        CompletableFuture.runAsync(() -> refreshSingleUserStatus(username));

        // Retourner la dernière valeur connue ou false par défaut
        return cachedStatus != null ? cachedStatus.isOnline() : false;
    }

    /**
     * *** MÉTHODE OPTIMISÉE POUR VÉRIFICATION D'EXISTENCE UTILISATEUR ***
     */
    public boolean checkUserExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        // Vérifier le cache d'abord
        CachedUserExists cachedExists = userExistsCache.get(username);
        if (cachedExists != null && !cachedExists.isExpired()) {
            return cachedExists.exists();
        }

        // Faire la requête de manière synchrone mais avec timeout court
        try {
            return checkUserExistsSynchronous(username);
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de l'utilisateur " + username + ": " + e.getMessage());
            // Retourner la dernière valeur connue ou false par défaut
            return cachedExists != null ? cachedExists.exists() : false;
        }
    }

    /**
     * *** REQUÊTE PAR LOT POUR VÉRIFIER PLUSIEURS UTILISATEURS ***
     */
    public CompletableFuture<Map<String, Boolean>> batchCheckOnlineStatus(List<String> usernames) {
        if (usernames.isEmpty() || !isConnected.get()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        Map<String, Boolean> results = new HashMap<>();
        List<String> usersToCheck = new ArrayList<>();

        // D'abord, utiliser le cache pour les valeurs disponibles
        for (String username : usernames) {
            CachedStatus status = onlineStatusCache.get(username);
            if (status != null && !status.isExpired()) {
                results.put(username, status.isOnline());
            } else {
                usersToCheck.add(username);
            }
        }

        if (usersToCheck.isEmpty()) {
            return CompletableFuture.completedFuture(results);
        }

        // Créer une clé unique pour cette requête par lot
        String batchKey = String.join(",", usersToCheck);

        // Vérifier si une requête similaire est déjà en cours
        CompletableFuture<Map<String, Boolean>> existingFuture = pendingBatchRequests.get(batchKey);
        if (existingFuture != null) {
            return existingFuture.thenApply(batchResults -> {
                Map<String, Boolean> combinedResults = new HashMap<>(results);
                combinedResults.putAll(batchResults);
                return combinedResults;
            });
        }

        // Créer une nouvelle requête par lot
        CompletableFuture<Map<String, Boolean>> future = CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("usernames", String.join(",", usersToCheck));
                payload.put("batch", "true");
                payload.put("timestamp", String.valueOf(System.currentTimeMillis()));

                synchronized (sendLock) {
                    PeerRequest request = new PeerRequest(RequestType.CHECK_ONLINE, payload);
                    sendRequest(request);

                    // Timeout plus généreux pour les requêtes par lot
                    socket.setSoTimeout(10000); // 10 secondes
                    PeerResponse response = readResponse();
                    socket.setSoTimeout(SOCKET_TIMEOUT); // Restaurer le timeout normal

                    Map<String, Boolean> batchResults = new HashMap<>();
                    if (response.isSuccess() && response.getData() instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) response.getData();

                        for (String username : usersToCheck) {
                            boolean online = false;
                            if (data.containsKey(username)) {
                                online = Boolean.parseBoolean(data.get(username).toString());
                            }

                            batchResults.put(username, online);
                            // Mettre en cache avec une durée de vie plus longue
                            onlineStatusCache.put(username, new CachedStatus(online, System.currentTimeMillis() + ONLINE_CACHE_DURATION));
                        }
                    } else {
                        // En cas d'échec, marquer tous comme hors ligne
                        for (String username : usersToCheck) {
                            batchResults.put(username, false);
                            onlineStatusCache.put(username, new CachedStatus(false, System.currentTimeMillis() + 30000)); // Cache court en cas d'erreur
                        }
                    }

                    return batchResults;
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la vérification par lot: " + e.getMessage());
                // En cas d'erreur, retourner tous comme hors ligne
                Map<String, Boolean> errorResults = new HashMap<>();
                for (String username : usersToCheck) {
                    errorResults.put(username, false);
                }
                return errorResults;
            } finally {
                // Nettoyer la requête en cours
                pendingBatchRequests.remove(batchKey);
                try {
                    socket.setSoTimeout(SOCKET_TIMEOUT);
                } catch (Exception ignored) {}
            }
        });

        pendingBatchRequests.put(batchKey, future);

        return future.thenApply(batchResults -> {
            Map<String, Boolean> combinedResults = new HashMap<>(results);
            combinedResults.putAll(batchResults);
            return combinedResults;
        });
    }

    /**
     * *** MÉTHODE PRIVÉE POUR VÉRIFICATION SYNCHRONE D'EXISTENCE ***
     */
    private boolean checkUserExistsSynchronous(String username) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);

        synchronized (sendLock) {
            PeerRequest request = new PeerRequest(RequestType.CHECK_USER, payload);
            sendRequest(request);

            // Timeout court pour vérification d'existence
            socket.setSoTimeout(5000); // 5 secondes
            PeerResponse response = readResponse();
            socket.setSoTimeout(SOCKET_TIMEOUT); // Restaurer le timeout normal

            boolean exists = false;
            if (response.isSuccess() && response.getData() instanceof Map) {
                Map<String, String> data = (Map<String, String>) response.getData();
                exists = "true".equals(data.get("exists"));
            }

            // Mettre en cache avec une longue durée de vie
            userExistsCache.put(username, new CachedUserExists(exists, System.currentTimeMillis() + USER_EXISTS_CACHE_DURATION));
            return exists;
        }
    }

    /**
     * *** RAFRAÎCHISSEMENT ASYNCHRONE DU STATUT D'UN UTILISATEUR ***
     */
    private void refreshSingleUserStatus(String username) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("timestamp", String.valueOf(System.currentTimeMillis()));

            synchronized (sendLock) {
                PeerRequest request = new PeerRequest(RequestType.CHECK_ONLINE, payload);
                sendRequest(request);

                socket.setSoTimeout(5000); // 5 secondes
                PeerResponse response = readResponse();
                socket.setSoTimeout(SOCKET_TIMEOUT);

                boolean isOnline = false;
                if (response.isSuccess() && response.getData() instanceof Map) {
                    Map<String, String> data = (Map<String, String>) response.getData();
                    isOnline = "true".equals(data.get("online"));
                }

                // Mettre en cache
                onlineStatusCache.put(username, new CachedStatus(isOnline, System.currentTimeMillis() + ONLINE_CACHE_DURATION));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du rafraîchissement du statut de " + username + ": " + e.getMessage());
        } finally {
            try {
                socket.setSoTimeout(SOCKET_TIMEOUT);
            } catch (Exception ignored) {}
        }
    }

    /**
     * *** NETTOYAGE DU CACHE EXPIRÉ ***
     */
    private void cleanExpiredCache() {
        long now = System.currentTimeMillis();

        // Nettoyer le cache des statuts en ligne
        onlineStatusCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));

        // Nettoyer le cache d'existence des utilisateurs
        userExistsCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));

        System.out.println("Cache nettoyé. Statuts en ligne: " + onlineStatusCache.size() +
                ", Existence utilisateurs: " + userExistsCache.size());
    }

    // *** MÉTHODES DE BASE INCHANGÉES ***
    public synchronized void sendRequest(PeerRequest request) throws IOException {
        if (!isConnected.get()) {
            throw new IOException("Non connecté au serveur");
        }

        synchronized (sendLock) {
            try {
                String json = gson.toJson(request);
                output.write(json + "\n");
                output.flush();
            } catch (IOException e) {
                isConnected.set(false);
                throw e;
            }
        }
    }

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
            throw e;
        } catch (IOException e) {
            isConnected.set(false);
            throw e;
        }
    }

    public void startMessageListener(ChatView chatView, String username) {
        if (messageListener != null && messageListener.isAlive()) {
            messageListener.stopListening();
            try {
                messageListener.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (isConnected.get()) {
            messageListener = new MessageListener(input, chatView, username);
            messageListener.start();
        }
    }

    public void stopMessageListener() {
        if (messageListener != null) {
            messageListener.stopListening();
            try {
                messageListener.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            messageListener = null;
        }
    }

    public boolean isMessageListenerRunning() {
        return messageListener != null && messageListener.isAlive();
    }

    public void closeConnection() {
        try {
            stopMessageListener();
            batchScheduler.shutdown();

            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();

            isConnected.set(false);
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }

    // *** CLASSES INTERNES POUR LE CACHE ***
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

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }

        public boolean isExpired(long now) {
            return now > expiryTime;
        }
    }

    private static class CachedUserExists {
        private final boolean exists;
        private final long expiryTime;

        public CachedUserExists(boolean exists, long expiryTime) {
            this.exists = exists;
            this.expiryTime = expiryTime;
        }

        public boolean exists() {
            return exists;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }

        public boolean isExpired(long now) {
            return now > expiryTime;
        }
    }
}