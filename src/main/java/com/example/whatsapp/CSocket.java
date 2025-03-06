package com.example.whatsapp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class CSocket {

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private Consumer<String> onMessageReceived; // Callback pour les messages entrants
    private Consumer<String> onFileReceived;   // Callback pour les fichiers entrants

    public CSocket(int serverPort, Consumer<String> onMessageReceived, Consumer<String> onFileReceived) throws IOException {
        this.onMessageReceived = onMessageReceived;
        this.onFileReceived = onFileReceived;

        // Accepter la connexion du client
        ServerSocket serverSocket = new ServerSocket(serverPort);
        System.out.println("Serveur en attente de connexion...");
        socket = serverSocket.accept();
        System.out.println("Client connecté : " + socket.getInetAddress());

        // Initialiser les streams
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());

        // Démarrer un thread pour écouter les messages entrants
        new Thread(this::listenForMessages).start();
    }

    public void sendMessage(String message) throws IOException {
        output.writeUTF(message);
        output.flush();
    }

    public void sendFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            output.writeUTF("file"); // Indiquer qu'un fichier est envoyé
            output.writeLong(file.length());
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("Fichier envoyé avec succès !");
        } else {
            System.out.println("Fichier introuvable !");
        }
    }

    private void listenForMessages() {
        try {
            while (true) {
                String message = input.readUTF(); // Lire le message
                if (message.equalsIgnoreCase("file")) {
                    String fileName = "recu_du_client_" + System.currentTimeMillis();
                    receiveFile(fileName, input);
                    onFileReceived.accept(fileName); // Notifier l'interface graphique
                } else {
                    onMessageReceived.accept(message); // Notifier l'interface graphique
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFile(String savePath, DataInputStream input) throws IOException {
        long fileSize = input.readLong();
        try (FileOutputStream fos = new FileOutputStream(savePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;
            while (totalBytesRead < fileSize && (bytesRead = input.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            System.out.println("Fichier reçu avec succès : " + savePath);
        }
    }

    public void close() throws IOException {
        if (socket != null) socket.close();
        if (input != null) input.close();
        if (output != null) output.close();
    }
}