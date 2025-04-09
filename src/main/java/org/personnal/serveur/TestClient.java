package org.personnal.serveur;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

    public static void main(String[] args) {
        try (
                Socket socket = new Socket("localhost", 5000);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("✅ Connecté au serveur (" + socket.getInetAddress() + ":" + socket.getPort() + ")");

            boolean running = true;
            while (running) {
                printMenu();
                String choix = scanner.nextLine();

                switch (choix) {
                    case "1":
                        handleLogin(output, input, scanner);
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

    private static void handleLogin(BufferedWriter output, BufferedReader input, Scanner scanner)
            throws IOException {
        Map<String, String> credentials = getCredentials(scanner);
        PeerRequest request = new PeerRequest(RequestType.LOGIN, credentials);

        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);
        System.out.println(response.isSuccess() ? "✅ " + response.getMessage() : "❌ " + response.getMessage());
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
        output.newLine(); // très important pour que le serveur lise la ligne complète
        output.flush();
    }

    private static PeerResponse receiveResponse(BufferedReader input) throws IOException {
        String json = input.readLine();
        Type responseType = new TypeToken<PeerResponse>(){}.getType();
        return gson.fromJson(json, responseType);
    }
}
