package com.example.whatsapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class FileSenderThread extends Thread {
    private Socket socket;
    private File file;

    public FileSenderThread(Socket socket, File file) {
        this.socket = socket;
        this.file = file;
    }

    @Override
    public void run() {
        try (FileInputStream fileInputStream = new FileInputStream(file);
             OutputStream outputStream = socket.getOutputStream()) {
            // Envoyer le nom du fichier
            String fileName = file.getName();
            byte[] fileNameBytes = fileName.getBytes();
            outputStream.write(fileNameBytes, 0, fileNameBytes.length);

            // Envoyer le contenu du fichier
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("Fichier envoyé : " + file.getName());
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi du fichier : " + e.getMessage());
        }
    }
}
