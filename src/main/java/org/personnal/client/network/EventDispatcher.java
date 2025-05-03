package org.personnal.client.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class EventDispatcher {
    private static final Gson gson = new Gson();
    public static String username;
    public static String password;

    public static void startSession(BufferedWriter output, BufferedReader input, TextArea messageArea) {
        Thread listenerThread = new Thread(() -> {
            try {
                while (true) {
                    String responseJson = input.readLine();
                    if (responseJson == null) break;

                    PeerResponse response = gson.fromJson(responseJson, PeerResponse.class);
                    if (response.getData() instanceof Map<?, ?> dataMap) {
                        String sender = (String) dataMap.get("sender");
                        String content = (String) dataMap.get("content");
                        Platform.runLater(() ->
                                messageArea.appendText("📩 Nouveau message de " + sender + ": " + content + "\n"));
                    } else {
                        Platform.runLater(() ->
                                messageArea.appendText("🧭 " + response.getMessage() + "\n"));
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() ->
                        messageArea.appendText("❌ Erreur de connexion: " + e.getMessage() + "\n"));
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public static void sendMessage(String receiver, String content, BufferedWriter output) throws IOException {
        Map<String, String> payload = new HashMap<>();
        payload.put("sender", username);
        payload.put("receiver", receiver);
        payload.put("content", content);
        payload.put("read", "false");

        PeerRequest request = new PeerRequest(RequestType.SEND_MESSAGE, payload);
        sendRequest(output, request);
    }


    public static void disconnect(BufferedWriter output, BufferedReader input) throws IOException {
        PeerRequest request = new PeerRequest(RequestType.DISCONNECT, null);
        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);

    }


    public static boolean handleLogin(BufferedWriter output, BufferedReader input) throws IOException {
        Map<String, String> credentials = getCredentials(username, password);
        PeerRequest request = new PeerRequest(RequestType.LOGIN, credentials);
        sendRequest(output, request);
        return receiveResponse(input).isSuccess();
    }

    public static void handleRegister(BufferedWriter output, BufferedReader input) throws IOException {
        Map<String, String> credentials = getCredentials(username, password);
        PeerRequest request = new PeerRequest(RequestType.REGISTER, credentials);
        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);
        System.out.println(response.isSuccess() ? "✅ " + response.getMessage() : "❌ " + response.getMessage());
    }

    private static Map<String, String> getCredentials(String username, String password) {
        Map<String, String> creds = new HashMap<>();
        creds.put("username", username);
        creds.put("password", password);
        return creds;
    }

    private static void sendRequest(BufferedWriter output, PeerRequest request) throws IOException {
        output.write(gson.toJson(request));
        output.newLine();
        output.flush();
    }

    private static PeerResponse receiveResponse(BufferedReader input) throws IOException {
        return gson.fromJson(input.readLine(), new TypeToken<PeerResponse>(){}.getType());
    }
}
