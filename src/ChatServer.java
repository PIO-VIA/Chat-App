import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 5000;
    private static Map<String, ClientHandler> clients = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🚀 Serveur de chat démarré sur le port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ Nouvelle connexion : " + clientSocket);

                // Démarrer un thread pour gérer le client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Erreur du serveur : " + e.getMessage());
        }
    }

    // Ajouter un client au registre
    public static synchronized boolean registerClient(String username, ClientHandler handler) {
        if (!clients.containsKey(username)) {
            clients.put(username, handler);
            return true;
        }
        return false;
    }

    // Supprimer un client
    public static synchronized void removeClient(String username) {
        clients.remove(username);
    }

    // Envoyer un message privé
    public static synchronized boolean sendMessageTo(String recipient, String sender, String message) {
        if (clients.containsKey(recipient)) {
            clients.get(recipient).sendMessage("📩 [Privé] de " + sender + ": " + message);
            return true;
        }
        return false;
    }
}
