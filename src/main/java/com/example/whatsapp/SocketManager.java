package com.example.whatsapp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketManager {
    private Socket clientSocket;
    private ServerSocket serverSocket;
    private String saveDirectory = System.getProperty("user.home") + "/Downloads"; // Répertoire de sauvegarde

    // Méthode pour démarrer un serveur
    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Serveur démarré sur le port " + port);
            clientSocket = serverSocket.accept();
            System.out.println("Client connecté : " + clientSocket.getInetAddress());
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur : " + e.getMessage());
        }
    }

    // Méthode pour se connecter à un serveur
    public void connectToServer(String host, int port) {
        try {
            clientSocket = new Socket(host, port);
            new MessageReceiverThread(clientSocket).start();
            new FileReceiverThread(clientSocket, saveDirectory).start();
            System.out.println("Connecté au serveur : " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("Erreur de connexion au serveur : " + e.getMessage());
        }
    }

    // Méthode pour fermer les sockets
    public void close() {
        try {
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
            System.out.println("Connexion fermée");
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture des sockets : " + e.getMessage());
        }
    }
}