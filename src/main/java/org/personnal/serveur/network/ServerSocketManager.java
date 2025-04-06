package org.personnal.serveur.network;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerSocketManager {

    private static final int PORT = 5000;
    private boolean running = true;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();

    public void start() {
        System.out.println("🔌 Serveur en cours de démarrage sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ Nouveau client connecté : " + clientSocket.getInetAddress());
                clientPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur serveur : " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        clientPool.shutdown();
    }
}
