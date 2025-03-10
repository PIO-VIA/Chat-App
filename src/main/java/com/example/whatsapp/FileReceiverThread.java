package com.example.whatsapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class FileReceiverThread extends Thread {
    private Socket socket;
    private String saveDirectory; // Répertoire de sauvegarde des fichiers reçus

    public FileReceiverThread(Socket socket, String saveDirectory) {
        this.socket = socket;
        this.saveDirectory = saveDirectory;
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream()) {
            // Lire le nom du fichier
            byte[] fileNameBytes = new byte[256];
            int fileNameLength = inputStream.read(fileNameBytes);
            String fileName = new String(fileNameBytes, 0, fileNameLength).trim();

            // Créer le fichier de destination
            File file = new File(saveDirectory, fileName);
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead); // Écrire les données dans le fichier
                }
            }
            System.out.println("Fichier reçu : " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erreur lors de la réception du fichier : " + e.getMessage());
        }
    }
}