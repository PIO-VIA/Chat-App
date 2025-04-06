package org.personnal.serveur;

import org.personnal.serveur.protocol.PeerRequest;
import org.personnal.serveur.protocol.PeerResponse;
import org.personnal.serveur.protocol.RequestType;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TestClient {

    public static void main(String[] args) {
        try (
                Socket socket = new Socket("localhost", 5000);
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
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

        } catch (IOException | ClassNotFoundException e) {
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

    private static void handleLogin(ObjectOutputStream output, ObjectInputStream input, Scanner scanner)
            throws IOException, ClassNotFoundException {
        Map<String, String> credentials = getCredentials(scanner);
        PeerRequest request = new PeerRequest(RequestType.LOGIN, credentials);

        output.writeObject(request);
        output.flush();

        PeerResponse response = (PeerResponse) input.readObject();
        System.out.println(response.isSuccess() ? "✅ " + response.getMessage() : "❌ " + response.getMessage());
    }

    private static void handleRegister(ObjectOutputStream output, ObjectInputStream input, Scanner scanner)
            throws IOException, ClassNotFoundException {
        Map<String, String> credentials = getCredentials(scanner);
        PeerRequest request = new PeerRequest(RequestType.REGISTER, credentials);

        output.writeObject(request);
        output.flush();

        PeerResponse response = (PeerResponse) input.readObject();
        System.out.println(response.isSuccess() ? "✅ " + response.getMessage() : "❌ " + response.getMessage());
    }

    private static void sendQuit(ObjectOutputStream output, ObjectInputStream input)
            throws IOException, ClassNotFoundException {
        PeerRequest request = new PeerRequest(RequestType.QUIT, null);
        output.writeObject(request);
        output.flush();
        PeerResponse response = (PeerResponse) input.readObject();
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
}
