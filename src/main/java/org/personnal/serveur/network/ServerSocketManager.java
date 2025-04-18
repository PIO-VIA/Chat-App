package org.personnal.serveur.network;

import org.personnal.serveur.config.ServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerSocketManager {

    private boolean running = true;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();

    public void start() {
        int port = ServerConfig.getPort();
        System.out.println("🔌 Serveur en cours de démarrage sur le port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ Nouveau client connecté : " + clientSocket.getInetAddress());
                clientPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur serveur : " + e.getMessage());
        } finally {
            stop();  // s'assure que le pool est fermé proprement
        }
    }

    public void stop() {
        running = false;
        clientPool.shutdown();
        System.out.println("🛑 Serveur arrêté et pool de threads fermé.");
    }
}
