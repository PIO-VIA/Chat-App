package org.personnal.serveur;

import org.personnal.serveur.protocol.PeerRequest;
import org.personnal.serveur.protocol.PeerResponse;
import org.personnal.serveur.protocol.RequestType;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.Gson;

public class CallTest {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final Gson gson = new Gson();

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;
    private volatile boolean connected = false;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java CallTest <username> [<password>]");
            return;
        }

        String username = args[0];
        String password = args.length > 1 ? args[1] : "password123"; // Mot de passe par défaut

        CallTest client = new CallTest();
        if (client.connect()) {
            System.out.println("✅ Connecté au serveur");

            // Connexion avec le compte utilisateur
            if (client.login(username, password)) {
                client.username = username;
                System.out.println("✅ Connecté en tant que: " + username);

                // Démarrer un thread pour lire les réponses du serveur
                client.startResponseListener();

                // Menu interactif
                client.showMenu();
            } else {
                System.out.println("❌ Échec de connexion");
                client.disconnect();
            }
        } else {
            System.out.println("❌ Impossible de se connecter au serveur");
        }
    }

    private boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connected = true;
            return true;
        } catch (IOException e) {
            System.err.println("❌ Erreur de connexion: " + e.getMessage());
            return false;
        }
    }

    private void disconnect() {
        try {
            if (connected) {
                // Envoyer une requête de déconnexion
                Map<String, String> payload = new HashMap<>();
                PeerRequest request = new PeerRequest(RequestType.DISCONNECT, payload);
                sendRequest(request);

                connected = false;
                socket.close();
                System.out.println("👋 Déconnecté du serveur");
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    private boolean login(String username, String password) {
        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);

        PeerRequest request = new PeerRequest(RequestType.LOGIN, payload);
        PeerResponse response = sendRequestAndWaitResponse(request);

        return response != null && response.isSuccess();
    }

    private void sendRequest(PeerRequest request) {
        try {
            String jsonRequest = gson.toJson(request);
            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("❌ Erreur d'envoi: " + e.getMessage());
        }
    }

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
            return null;
        }
    }

    private void startResponseListener() {
        new Thread(() -> {
            try {
                while (connected) {
                    String jsonResponse = reader.readLine();
                    if (jsonResponse == null) {
                        System.out.println("⚠️ Connexion fermée par le serveur");
                        connected = false;
                        break;
                    }

                    PeerResponse response = gson.fromJson(jsonResponse, PeerResponse.class);
                    handleServerResponse(response);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("❌ Erreur de lecture: " + e.getMessage());
                    connected = false;
                }
            }
        }).start();
    }

    private void handleServerResponse(PeerResponse response) {
        if (response.getMessage().contains("Appel entrant")) {
            // Gérer un appel entrant
            Map<String, String> data = (Map<String, String>) response.getData();
            String caller = data.get("caller");
            System.out.println("\n📞 Appel entrant de " + caller);
            System.out.println("Tapez 'accept " + caller + "' pour accepter ou 'reject " + caller + "' pour rejeter");
        } else if (response.getMessage().contains("Signal d'appel")) {
            // Gérer les signaux WebRTC
            Map<String, String> data = (Map<String, String>) response.getData();
            String action = data.get("action");
            String from = data.get("from");

            System.out.println("\n📡 Signal " + action + " reçu de " + from);

            // En production, ces signaux seraient traités par la bibliothèque WebRTC
            // Ici, on simule une réponse automatique pour le test
            if (action.equals("offer")) {
                // Simuler une réponse à l'offre
                sendCallSignal(from, "answer", "{\"type\":\"answer\",\"sdp\":\"simulated-sdp\"}");
            }
        } else if (response.getMessage().contains("Appel accepté")) {
            System.out.println("\n✅ Appel accepté! Connexion en cours...");
            // En production, ici on démarre les flux audio

            // Simuler l'envoi d'une offre SDP
            Map<String, String> data = (Map<String, String>) response.getData();
            String callee = data.get("callee");
            sendCallSignal(callee, "offer", "{\"type\":\"offer\",\"sdp\":\"simulated-sdp\"}");
        } else if (response.getMessage().contains("Appel rejeté")) {
            System.out.println("\n❌ Appel rejeté par l'utilisateur");
        } else if (response.getMessage().contains("Appel terminé")) {
            System.out.println("\n🛑 L'appel a été terminé par l'autre utilisateur");
        } else {
            // Afficher simplement les autres messages
            System.out.println("\nRéponse du serveur: " + response.getMessage());
        }
    }

    private void initiateCall(String callee) {
        Map<String, String> payload = new HashMap<>();
        payload.put("caller", username);
        payload.put("callee", callee);
        payload.put("action", "initiate");

        PeerRequest request = new PeerRequest(RequestType.CALL, payload);
        sendRequest(request);

        System.out.println("📞 Appel vers " + callee + " en cours...");
    }

    private void acceptCall(String caller) {
        Map<String, String> payload = new HashMap<>();
        payload.put("caller", caller);
        payload.put("callee", username);
        payload.put("action", "accept");

        PeerRequest request = new PeerRequest(RequestType.CALL, payload);
        sendRequest(request);

        System.out.println("✅ Appel accepté");
    }

    private void rejectCall(String caller) {
        Map<String, String> payload = new HashMap<>();
        payload.put("caller", caller);
        payload.put("callee", username);
        payload.put("action", "reject");

        PeerRequest request = new PeerRequest(RequestType.CALL, payload);
        sendRequest(request);

        System.out.println("❌ Appel rejeté");
    }

    private void hangupCall(String otherUser) {
        Map<String, String> payload = new HashMap<>();
        payload.put("caller", username);
        payload.put("callee", otherUser);
        payload.put("action", "hangup");

        PeerRequest request = new PeerRequest(RequestType.CALL, payload);
        sendRequest(request);

        System.out.println("🛑 Appel terminé");
    }

    private void sendCallSignal(String target, String signalType, String data) {
        Map<String, String> payload = new HashMap<>();
        payload.put("caller", username);
        payload.put("callee", target);
        payload.put("action", signalType);
        payload.put("data", data);

        PeerRequest request = new PeerRequest(RequestType.CALL, payload);
        sendRequest(request);

        System.out.println("📡 Signal " + signalType + " envoyé");
    }

    private void showMenu() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== Menu Client d'Appel ===");
        System.out.println("Commandes disponibles :");
        System.out.println("call <username> - Appeler un utilisateur");
        System.out.println("accept <username> - Accepter un appel entrant");
        System.out.println("reject <username> - Rejeter un appel entrant");
        System.out.println("hangup <username> - Terminer un appel en cours");
        System.out.println("exit - Quitter le programme");

        boolean running = true;
        while (running && connected) {
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
            } else if (input.startsWith("hangup ")) {
                String otherUser = input.substring(7).trim();
                hangupCall(otherUser);
            } else {
                System.out.println("❌ Commande non reconnue");
            }
        }

        scanner.close();
    }
}