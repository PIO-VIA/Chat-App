import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Demander un pseudo unique
            while (true) {
                out.println("🔹 Entrez un pseudo unique : ");
                username = in.readLine();
                if (username == null || username.trim().isEmpty()) continue;

                synchronized (ChatServer.class) {
                    if (ChatServer.registerClient(username, this)) {
                        out.println("✅ Connecté en tant que " + username);
                        System.out.println(username + " a rejoint le chat.");
                        break;
                    } else {
                        out.println("❌ Pseudo déjà utilisé, essayez un autre.");
                    }
                }
            }

            // Écouter les messages du client
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/msg ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length == 3) {
                        String recipient = parts[1];
                        String msgContent = parts[2];
                        if (!ChatServer.sendMessageTo(recipient, username, msgContent)) {
                            out.println("❌ Utilisateur introuvable.");
                        }
                    } else {
                        out.println("⚠️ Format : /msg [pseudo] [message]");
                    }
                } else {
                    out.println("❌ Commande invalide. Utilisez /msg [pseudo] [message]");
                }
            }
        } catch (IOException e) {
            System.out.println("Erreur avec " + username + " : " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ChatServer.removeClient(username);
            System.out.println(username + " s'est déconnecté.");
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
