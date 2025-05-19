package org.personnal.serveur.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.personnal.serveur.auth.IUserService;
import org.personnal.serveur.auth.UserServiceImpl;
import org.personnal.serveur.model.Message;
import org.personnal.serveur.model.User;
import org.personnal.serveur.protocol.PeerRequest;
import org.personnal.serveur.protocol.PeerResponse;
import org.personnal.serveur.protocol.RequestType;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final IUserService userService = new UserServiceImpl();
    private final Gson gson = new Gson();
    private String username;
    private BufferedReader reader;
    private BufferedWriter writer;

    // Timestamp de la dernière activité pour détecter les clients inactifs
    private long lastActivityTimestamp;

    // Cache pour les sessions typant
    private static final ConcurrentHashMap<String, TypingStatus> typingStatusMap = new ConcurrentHashMap<>();

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.lastActivityTimestamp = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            boolean running = true;

            while (running) {
                String jsonLine = reader.readLine();
                if (jsonLine == null) break;

                // Mettre à jour le timestamp d'activité
                this.lastActivityTimestamp = System.currentTimeMillis();

                PeerRequest request;
                try {
                    request = gson.fromJson(jsonLine, PeerRequest.class);
                } catch (JsonSyntaxException e) {
                    System.err.println("❌ Requête JSON invalide : " + e.getMessage());
                    sendJsonResponse(new PeerResponse(false, "❌ Format JSON invalide"));
                    continue;
                }

                if (request.getType() == RequestType.DISCONNECT) {
                    running = false;
                    sendJsonResponse(new PeerResponse(true, "👋 Déconnexion réussie"));
                    break;
                }

                PeerResponse response = handleRequest(request);
                sendJsonResponse(response);
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur côté serveur (ClientHandler) : " + e.getMessage());
        } finally {
            if (username != null) {
                SessionManager.removeUser(username);

                // Nettoyer le statut de typing
                typingStatusMap.remove(username);
            }
            try {
                clientSocket.close();
                System.out.println("✅ Connexion fermée avec le client");
            } catch (IOException e) {
                System.err.println("❌ Erreur fermeture socket : " + e.getMessage());
            }
        }
    }

    private void sendJsonResponse(PeerResponse response) throws IOException {
        String jsonResponse = gson.toJson(response);
        writer.write(jsonResponse);
        writer.newLine();
        writer.flush();
    }

    private PeerResponse handleRequest(PeerRequest request) {
        switch (request.getType()) {
            case LOGIN:
                return handleLogin(request.getPayload());
            case REGISTER:
                return handleRegister(request.getPayload());
            case SEND_MESSAGE:
                return handleSendMessage(request.getPayload());
            case SEND_FILE:
                return handleSendFile(request.getPayload());
            case CHECK_USER:
                return handleCheckUser(request.getPayload());
            case CHECK_ONLINE:
                return handleCheckOnline(request.getPayload());
            case PING:
                return handlePing(request.getPayload());
            case TYPING:
                return handleTypingStatus(request.getPayload());
            case DISCONNECT:
                return new PeerResponse(true, "👋 Déconnecté proprement.");
            default:
                return new PeerResponse(false, "❌ Type de requête inconnu");
        }
    }

    /**
     * Gère les requêtes de ping du client
     * @param payload Les données du ping (peuvent contenir des infos additionnelles)
     * @return Une réponse confirmant la réception du ping
     */
    private PeerResponse handlePing(Map<String, String> payload) {
        // Log le ping si nécessaire pour le débogage
        // System.out.println("Ping reçu de " + (username != null ? username : "client non identifié"));

        // Mettre à jour le timestamp
        this.lastActivityTimestamp = System.currentTimeMillis();

        // Renvoyer une réponse simple
        return new PeerResponse(true, "pong", Map.of("timestamp", String.valueOf(System.currentTimeMillis())));
    }

    /**
     * Gère les notifications de statut de frappe (typing)
     * @param payload Les données contenant l'émetteur et le destinataire
     * @return Une réponse confirmant la réception
     */
    private PeerResponse handleTypingStatus(Map<String, String> payload) {
        String sender = payload.get("sender");
        String receiver = payload.get("receiver");
        boolean isTyping = Boolean.parseBoolean(payload.get("typing"));

        if (sender == null || receiver == null) {
            return new PeerResponse(false, "❌ Données de typing incomplètes");
        }

        // Mettre à jour le statut dans la map
        typingStatusMap.put(sender, new TypingStatus(receiver, isTyping, System.currentTimeMillis()));

        // Transmettre le statut au destinataire s'il est connecté
        ClientHandler receiverHandler = SessionManager.getUserHandler(receiver);
        if (receiverHandler != null) {
            try {
                receiverHandler.sendJsonResponse(
                        new PeerResponse(true, "typing_status", Map.of(
                                "from", sender,
                                "typing", String.valueOf(isTyping)
                        ))
                );
            } catch (IOException e) {
                System.err.println("❌ Erreur lors de l'envoi du statut typing: " + e.getMessage());
            }
        }

        return new PeerResponse(true, "✅ Statut de frappe mis à jour");
    }

    private PeerResponse handleLogin(Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        User user = userService.login(username, password);
        if (user != null) {
            this.username = username;
            SessionManager.addUser(username, this);
            return new PeerResponse(true, "✅ Connexion réussie", user);
        } else {
            return new PeerResponse(false, "❌ Identifiants incorrects");
        }
    }

    private PeerResponse handleRegister(Map<String, String> payload) {
        String username = payload.get("username");
        String email = payload.get("email");
        String password = payload.get("password");

        User newUser = userService.register(username,email, password);
        if (newUser != null) {
            return new PeerResponse(true, "✅ Inscription réussie", newUser);
        } else {
            return new PeerResponse(false, "❌ Nom d'utilisateur déjà utilisé ou échec");
        }
    }

    private PeerResponse handleSendMessage(Map<String, String> payload) {
        String sender = payload.get("sender");
        String receiver = payload.get("receiver");
        String content = payload.get("content");
        boolean read = Boolean.parseBoolean(payload.get("read"));

        Message message = new Message(sender, receiver, content, System.currentTimeMillis(), read);

        ClientHandler receiverHandler = SessionManager.getUserHandler(receiver);
        if (receiverHandler != null) {
            try {
                receiverHandler.sendJsonResponse(
                        new PeerResponse(true, "📨 Nouveau message reçu", message)
                );
                return new PeerResponse(true, "✅ Message délivré à " + receiver, message);
            } catch (IOException e) {
                return new PeerResponse(false, "❌ Erreur d'envoi au destinataire : " + e.getMessage());
            }
        } else {
            return new PeerResponse(false, "❌ Utilisateur " + receiver + " non connecté");
        }
    }

    private PeerResponse handleSendFile(Map<String, String> payload) {
        String sender = payload.get("sender");
        String receiver = payload.get("receiver");
        String filename = payload.get("filename");
        String base64Content = payload.get("content");

        ClientHandler receiverHandler = SessionManager.getUserHandler(receiver);
        if (receiverHandler != null) {
            try {
                Map<String, String> filePayload = Map.of(
                        "from", sender,
                        "filename", filename,
                        "content", base64Content
                );
                PeerResponse response = new PeerResponse(true, "📁 Nouveau fichier de " + sender, filePayload);
                receiverHandler.sendJsonResponse(response);
                return new PeerResponse(true, "✅ Fichier délivré à " + receiver);
            } catch (IOException e) {
                return new PeerResponse(false, "❌ Erreur d'envoi de fichier : " + e.getMessage());
            }
        } else {
            return new PeerResponse(false, "❌ Utilisateur " + receiver + " non connecté");
        }
    }

    /**
     * Gère la requête de vérification d'existence d'un utilisateur
     * @param payload les données de la requête contenant le nom d'utilisateur à vérifier
     * @return une réponse indiquant si l'utilisateur existe ou non
     */
    private PeerResponse handleCheckUser(Map<String, String> payload) {
        String usernameToCheck = payload.get("username");

        if (usernameToCheck == null || usernameToCheck.trim().isEmpty()) {
            return new PeerResponse(false, "❌ Nom d'utilisateur non spécifié");
        }

        boolean userExists = userService.userExists(usernameToCheck);

        if (userExists) {
            return new PeerResponse(true, "✅ L'utilisateur " + usernameToCheck + " existe", Map.of("exists", "true"));
        } else {
            return new PeerResponse(false, "❌ L'utilisateur " + usernameToCheck + " n'existe pas", Map.of("exists", "false"));
        }
    }

    /**
     * Gère la requête de vérification si un utilisateur est en ligne
     * @param payload les données de la requête contenant le nom d'utilisateur à vérifier
     * @return une réponse indiquant si l'utilisateur est connecté ou non
     */
    private PeerResponse handleCheckOnline(Map<String, String> payload) {
        String usernameToCheck = payload.get("username");

        if ("ping_test".equals(usernameToCheck)) {
            return new PeerResponse(true, "ping_response", Map.of("timestamp", String.valueOf(System.currentTimeMillis())));
        }

        if (usernameToCheck == null || usernameToCheck.trim().isEmpty()) {
            return new PeerResponse(false, "❌ Nom d'utilisateur non spécifié");
        }

        boolean isOnline = SessionManager.isUserOnline(usernameToCheck);

        if (isOnline) {
            return new PeerResponse(true, "✅ L'utilisateur " + usernameToCheck + " est en ligne", Map.of("online", "true"));
        } else {
            return new PeerResponse(false, "❌ L'utilisateur " + usernameToCheck + " n'est pas en ligne", Map.of("online", "false"));
        }
    }

    // Méthode utilitaire pour envoyer un message texte d'un autre handler.
    public void sendTextMessage(String fromUsername, String message) {
        try {
            Map<String, String> payload = Map.of(
                    "from", fromUsername,
                    "content", message
            );
            sendJsonResponse(new PeerResponse(true, "💬 Nouveau message reçu", payload));
        } catch (IOException e) {
            System.err.println("❌ Erreur envoi message direct : " + e.getMessage());
        }
    }

    /**
     * Vérifie si ce client est inactif depuis trop longtemps
     * @param timeoutMs Le temps d'inactivité considéré comme un timeout (en ms)
     * @return true si le client doit être considéré comme inactif
     */
    public boolean isInactive(long timeoutMs) {
        return (System.currentTimeMillis() - lastActivityTimestamp) > timeoutMs;
    }

    /**
     * Obtient le timestamp de la dernière activité
     * @return Le timestamp en millisecondes
     */
    public long getLastActivityTimestamp() {
        return lastActivityTimestamp;
    }

    /**
     * Classe interne pour stocker les états de frappe
     */
    private static class TypingStatus {
        private final String receiver;
        private final boolean isTyping;
        private final long timestamp;

        public TypingStatus(String receiver, boolean isTyping, long timestamp) {
            this.receiver = receiver;
            this.isTyping = isTyping;
            this.timestamp = timestamp;
        }
    }
}