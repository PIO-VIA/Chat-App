package org.personnal.client.network;

import com.google.gson.Gson;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientSocketManager {
    private static ClientSocketManager instance;

    private Socket socket;
    private BufferedReader input;
    private BufferedWriter output;
    private final Gson gson = new Gson();

    // Constructeur privé pour singleton
    private ClientSocketManager() {}

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
            socket.setSoTimeout(30000);  // Timeout de lecture (30 sec)
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            System.out.println("✅ Connecté au serveur");
        } catch (IOException e) {
            System.err.println("❌ Erreur de connexion au serveur : " + e.getMessage());
            throw e;
        }
    }

    public synchronized void sendRequest(PeerRequest request) throws IOException {
        String json = gson.toJson(request);
        output.write(json + "\n");
        output.flush();
    }

    public synchronized PeerResponse readResponse() throws IOException {
        String responseJson = input.readLine();
        return gson.fromJson(responseJson, PeerResponse.class);
    }

    public void closeConnection() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("🔌 Connexion fermée");
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }
}
