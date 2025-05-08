package org.personnal.client.network;

import com.google.gson.Gson;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class ClientSocketManager {
    private static ClientSocketManager instance;

    private Socket socket;
    private BufferedReader input;
    private BufferedWriter output;
    private final Gson gson = new Gson();
    private Consumer<List<String>> usersUpdateListener;

    // Constructeur privé pour singleton
    private ClientSocketManager() {}

    public void setUsersUpdateListener(Consumer<List<String>> listener) {
        this.usersUpdateListener = listener;
    }

    public void startListeningThread() {
        System.out.println("Démarrage du thread d'écoute");
        new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    String responseJson = input.readLine();
                    if (responseJson == null) {
                        System.out.println("Connexion fermée par le serveur");
                        break;
                    }

                    System.out.println("Réponse reçue: " + responseJson);

                    PeerResponse response = gson.fromJson(responseJson, PeerResponse.class);

                    // Traitement de la réponse selon son type
                    if (response.getType() == RequestType.GET_CONNECTED_USERS && usersUpdateListener != null) {
                        try {
                            @SuppressWarnings("unchecked")
                            List<String> users = (List<String>) response.getData();
                            System.out.println("Utilisateurs reçus: " + users);
                            usersUpdateListener.accept(users);
                        } catch (ClassCastException e) {
                            System.err.println("Erreur de casting des données utilisateurs: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("Erreur dans le thread d'écoute: " + e.getMessage());
                }
            }
        }).start();
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
            socket = new Socket(host, port);
            socket.setSoTimeout(60000);   //Timeout de lecture (60 sec)
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
        System.out.println("Envoi de la requête: " + json);
        output.write(json + "\n");
        output.flush();
    }

    public synchronized PeerResponse readResponse() throws IOException {
        String responseJson = input.readLine();
        if (responseJson == null) {
            throw new IOException("Connexion fermée par le serveur");
        }
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