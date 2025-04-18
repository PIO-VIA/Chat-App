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
import org.personnal.serveur.network.SessionManager;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final IUserService userService = new UserServiceImpl();
    private final Gson gson = new Gson();
    private String username;
    private BufferedReader reader;
    private BufferedWriter writer;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
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
            case GET_CONNECTED_USERS:
                return handleGetConnectedUsers();
            case DISCONNECT:
                return new PeerResponse(true, "👋 Déconnecté proprement.");
            default:
                return new PeerResponse(false, "❌ Type de requête inconnu");
        }
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
        String password = payload.get("password");

        User newUser = userService.register(username, password);
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
                return new PeerResponse(false, "❌ Erreur d’envoi au destinataire : " + e.getMessage());
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
                return new PeerResponse(false, "❌ Erreur d’envoi de fichier : " + e.getMessage());
            }
        } else {
            return new PeerResponse(false, "❌ Utilisateur " + receiver + " non connecté");
        }
    }

    private PeerResponse handleGetConnectedUsers() {
        return new PeerResponse(true, "👥 Utilisateurs connectés",
                new ArrayList<>(SessionManager.getAllSessions().keySet()));
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
}
