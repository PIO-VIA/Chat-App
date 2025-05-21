package org.personnal.serveur.test;


import com.google.gson.Gson;
import org.personnal.serveur.protocol.PeerRequest;
import org.personnal.serveur.protocol.PeerResponse;
import org.personnal.serveur.protocol.RequestType;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientExample {

    private final String serverHost;
    private final int serverPort;
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final Gson gson = new Gson();

    public ClientExample(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void connect() throws IOException {
        socket = new Socket(serverHost, serverPort);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void disconnect() throws IOException {
        // Envoyer requête de déconnexion
        Map<String, String> payload = new HashMap<>();
        PeerRequest request = new PeerRequest(RequestType.DISCONNECT, payload);
        sendRequest(request);

        // Fermer les ressources
        reader.close();
        writer.close();
        socket.close();
    }

    private void sendRequest(PeerRequest request) throws IOException {
        String jsonRequest = gson.toJson(request);
        writer.write(jsonRequest);
        writer.newLine();
        writer.flush();
    }

    private PeerResponse receiveResponse() throws IOException {
        String jsonResponse = reader.readLine();
        return gson.fromJson(jsonResponse, PeerResponse.class);
    }

    // Exemple de vérification si un utilisateur existe
    public PeerResponse checkUserExists(String username) throws IOException {
        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);

        PeerRequest request = new PeerRequest(RequestType.CHECK_USER, payload);
        sendRequest(request);

        return receiveResponse();
    }

    // Exemple de vérification si un utilisateur est en ligne
    public PeerResponse checkUserOnline(String username) throws IOException {
        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);

        PeerRequest request = new PeerRequest(RequestType.CHECK_ONLINE, payload);
        sendRequest(request);

        return receiveResponse();
    }

    // Exemple d'utilisation
    public static void main(String[] args) {
        try {
            ClientExample client = new ClientExample("localhost", 5000);
            client.connect();

            // Vérifier si un utilisateur existe
            PeerResponse existsResponse = client.checkUserExists("pio");
            System.out.println("L'utilisateur existe: " + existsResponse.isSuccess());
            System.out.println("Message: " + existsResponse.getMessage());

            // Vérifier si un utilisateur est en ligne
            PeerResponse onlineResponse = client.checkUserOnline("pio");
            System.out.println("L'utilisateur est en ligne: " + onlineResponse.isSuccess());
            System.out.println("Message: " + onlineResponse.getMessage());

            client.disconnect();

        } catch (IOException e) {
            System.err.println("Erreur client: " + e.getMessage());
        }
    }
}