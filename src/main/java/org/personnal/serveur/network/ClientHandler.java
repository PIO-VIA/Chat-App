package org.personnal.serveur.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.personnal.serveur.auth.IUserService;
import org.personnal.serveur.auth.UserServiceImpl;
import org.personnal.serveur.model.User;
import org.personnal.serveur.protocol.PeerRequest;
import org.personnal.serveur.protocol.PeerResponse;
import org.personnal.serveur.protocol.RequestType;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final IUserService userService = new UserServiceImpl();
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            boolean running = true;

            while (running) {
                String jsonLine = reader.readLine();
                if (jsonLine == null) break;

                PeerRequest request;
                try {
                    request = gson.fromJson(jsonLine, PeerRequest.class);
                } catch (JsonSyntaxException e) {
                    System.err.println("❌ Requête JSON invalide : " + e.getMessage());
                    sendJsonResponse(writer, new PeerResponse(false, "❌ Format JSON invalide"));
                    continue;
                }

                if (request.getType() == RequestType.QUIT) {
                    running = false;
                    sendJsonResponse(writer, new PeerResponse(true, "👋 Déconnexion réussie"));
                    break;
                }

                PeerResponse response = handleRequest(request);
                sendJsonResponse(writer, response);
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur côté serveur (ClientHandler) : " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("✅ Connexion fermée avec le client");
            } catch (IOException e) {
                System.err.println("❌ Erreur fermeture socket : " + e.getMessage());
            }
        }
    }

    private void sendJsonResponse(BufferedWriter writer, PeerResponse response) throws IOException {
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
}
