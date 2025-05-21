package org.personnal.serveur.network;

import org.personnal.serveur.config.ServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerSocketManager {

    private volatile boolean running = true;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;

    public void start() {
        int port = ServerConfig.getPort();
        System.out.println("🔌 Serveur en cours de démarrage sur le port " + port);

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000); // Timeout de 1 seconde pour permettre la vérification de running

            System.out.println("✅ Serveur démarré et en attente de connexions");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("✅ Nouveau client connecté : " + clientSocket.getInetAddress());
                    clientPool.execute(new ClientHandler(clientSocket));
                } catch (SocketTimeoutException e) {
                    // Timeout normal, continue la boucle pour vérifier si running est toujours true
                    continue;
                } catch (SocketException e) {
                    if (!running) {
                        // Socket fermée intentionnellement lors de l'arrêt
                        break;
                    }
                    System.err.println("❌ Erreur de socket : " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("❌ Erreur d'E/S : " + e.getMessage());
                    if (!running) break;
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur serveur : " + e.getMessage());
        } finally {
            stop();  // s'assure que le pool est fermé proprement
        }
    }

    public void stop() {
        running = false;

        // Fermer d'abord la socket du serveur
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("🔌 Socket serveur fermée");
            } catch (IOException e) {
                System.err.println("❌ Erreur lors de la fermeture de la socket serveur : " + e.getMessage());
            }
        }

        // Arrêter le pool de threads
        clientPool.shutdown();
        try {
            if (!clientPool.awaitTermination(10, TimeUnit.SECONDS)) {
                clientPool.shutdownNow();
                if (!clientPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("❌ Le pool de threads ne s'est pas arrêté");
                }
            }
        } catch (InterruptedException e) {
            clientPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Arrêter le gestionnaire de sessions
        SessionManager.shutdown();

        // Arrêter le gestionnaire d'appels
        CallManager.shutdown();
        // Arrêter le cache des utilisateurs
        ClientHandler.shutdownCache();

        System.out.println("🛑 Serveur arrêté et ressources libérées.");
    }
    /**
     * Ajoute un hook d'arrêt pour fermer proprement le serveur
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("⚠️ Arrêt du serveur en cours...");
            stop();
        }));
    }
}