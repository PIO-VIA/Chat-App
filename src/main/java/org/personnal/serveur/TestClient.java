package org.personnal.serveur;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.personnal.serveur.model.Message;
import org.personnal.serveur.protocol.PeerRequest;
import org.personnal.serveur.protocol.PeerResponse;
import org.personnal.serveur.protocol.RequestType;

import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TestClient {

    private static final Gson gson = new Gson();
    private static String username;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket("localhost", 5000);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("✅ Connecté au serveur");

            boolean running = true;
            while (running) {
                printMenu();
                String choix = scanner.nextLine();

                switch (choix) {
                    case "1":
                        if (handleLogin(output, input, scanner)) {
                            startSession(output, input, scanner);
                        }
                        break;
                    case "2":
                        handleRegister(output, input, scanner);
                        break;
                    case "3":
                        sendQuit(output, input);
                        running = false;
                        break;
                    default:
                        System.out.println("❌ Choix invalide !");
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur côté client : " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\n=== Menu Principal ===");
        System.out.println("1. Se connecter");
        System.out.println("2. S'inscrire");
        System.out.println("3. Quitter");
        System.out.print("Votre choix > ");
    }

    private static boolean handleLogin(BufferedWriter output, BufferedReader input, Scanner scanner)
            throws IOException {
        Map<String, String> credentials = getCredentials(scanner);
        PeerRequest request = new PeerRequest(RequestType.LOGIN, credentials);

        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);

        if (response.isSuccess()) {
            username = credentials.get("username");
            System.out.println("✅ Connexion réussie !");
            return true;
        } else {
            System.out.println("❌ " + response.getMessage());
            return false;
        }
    }

    private static void handleRegister(BufferedWriter output, BufferedReader input, Scanner scanner)
            throws IOException {
        Map<String, String> credentials = getCredentials(scanner);
        PeerRequest request = new PeerRequest(RequestType.REGISTER, credentials);

        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);
        System.out.println(response.isSuccess() ? "✅ " + response.getMessage() : "❌ " + response.getMessage());
    }

    private static void sendQuit(BufferedWriter output, BufferedReader input) throws IOException {
        PeerRequest request = new PeerRequest(RequestType.DISCONNECT, null);
        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);
        System.out.println("👋 " + response.getMessage());
    }

    private static void startSession(BufferedWriter output, BufferedReader input, Scanner scanner) {
        // Thread d’écoute des messages entrants
        Thread listenerThread = new Thread(() -> {
            try {
                while (true) {
                    String responseJson = input.readLine();
                    if (responseJson == null) break;

                    PeerResponse response = gson.fromJson(responseJson, PeerResponse.class);
                    if (response.getData() instanceof Map<?, ?> dataMap) {
                        // Lecture d’un message (convertir manuellement)
                        System.out.println("📩 Nouveau message : " + dataMap.get("content") +
                                " (de " + dataMap.get("sender") + ")");
                    } else {
                        System.out.println("🧭 Réponse du serveur : " + response.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("❌ Erreur dans le thread d’écoute : " + e.getMessage());
            }
        });
        listenerThread.start();

        // Boucle de commandes utilisateur
        while (true) {
            System.out.print("\nCommande (send / disconnect) > ");
            String command = scanner.nextLine();

            if (command.equalsIgnoreCase("disconnect")) {
                try {
                    sendQuit(output, input);
                    break;
                } catch (IOException e) {
                    System.err.println("❌ Erreur de déconnexion : " + e.getMessage());
                }
            } else if (command.equalsIgnoreCase("send")) {
                try {
                    System.out.print("Destinataire : ");
                    String receiver = scanner.nextLine();
                    System.out.print("Message : ");
                    String content = scanner.nextLine();

                    Map<String, String> payload = new HashMap<>();
                    payload.put("sender", username);
                    payload.put("receiver", receiver);
                    payload.put("content", content);
                    payload.put("read", "false");

                    PeerRequest request = new PeerRequest(RequestType.SEND_MESSAGE, payload);
                    sendRequest(output, request);

                } catch (IOException e) {
                    System.err.println("❌ Erreur lors de l’envoi du message : " + e.getMessage());
                }
            } else {
                System.out.println("❌ Commande inconnue.");
            }
        }
    }

    private static Map<String, String> getCredentials(Scanner scanner) {
        Map<String, String> creds = new HashMap<>();
        System.out.print("Nom d'utilisateur : ");
        creds.put("username", scanner.nextLine());
        System.out.print("Mot de passe : ");
        creds.put("password", scanner.nextLine());
        return creds;
    }

    private static void sendRequest(BufferedWriter output, PeerRequest request) throws IOException {
        String json = gson.toJson(request);
        output.write(json);
        output.newLine();
        output.flush();
    }

    private static PeerResponse receiveResponse(BufferedReader input) throws IOException {
        String json = input.readLine();
        Type responseType = new TypeToken<PeerResponse>() {}.getType();
        return gson.fromJson(json, responseType);
    }
}
