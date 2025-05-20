package org.personnal.client.call;

import com.google.gson.Gson;
import javafx.application.Platform;
import org.personnal.client.controller.ChatController;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.RequestType;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CallSocketManager {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final Gson gson = new Gson();
    private final String serverHost;
    private final int serverPort;
    private final String username;

    private volatile boolean connected = false;
    private Thread listenerThread;

    // Écouteur d'événements pour les réponses
    private Consumer<Map<String, String>> responseListener;

    public CallSocketManager(String serverHost, int serverPort, String username) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.username = username;
    }

    public void setResponseListener(Consumer<Map<String, String>> listener) {
        this.responseListener = listener;
    }

    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connected = true;

            // Démarrer le thread d'écoute
            startListenerThread();

            return true;
        } catch (IOException e) {
            System.err.println("❌ Erreur de connexion pour les appels: " + e.getMessage());
            return false;
        }
    }

    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                while (connected) {
                    String jsonResponse = reader.readLine();
                    if (jsonResponse == null) {
                        break; // Connexion fermée
                    }

                    // Traiter la réponse
                    processResponse(jsonResponse);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("❌ Erreur de lecture pour les appels: " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void processResponse(String jsonResponse) {
        try {
            // Extraire les données pertinentes
            Map<String, Object> response = gson.fromJson(jsonResponse, Map.class);
            if (response != null && response.containsKey("data")) {
                Object data = response.get("data");
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> callData = (Map<String, String>) data;

                    // Notifier l'écouteur
                    if (responseListener != null) {
                        Platform.runLater(() -> responseListener.accept(callData));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur de traitement pour les appels: " + e.getMessage());
        }
    }

    public void sendCallRequest(String action, String target, String data) {
        if (!connected) {
            if (!connect()) {
                return;
            }
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("caller", username);
            payload.put("callee", target);
            payload.put("action", action);

            if (data != null) {
                payload.put("data", data);
            }

            PeerRequest request = new PeerRequest(RequestType.CALL, payload);
            String jsonRequest = gson.toJson(request);

            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("❌ Erreur d'envoi pour les appels: " + e.getMessage());
            disconnect();
        }
    }

    public void disconnect() {
        connected = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur de déconnexion pour les appels: " + e.getMessage());
        }
    }
}