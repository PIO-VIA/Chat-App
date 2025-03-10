package com.example.whatsapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class MessageSenderThread extends Thread {
    private Socket socket;
    private String message;

    public MessageSenderThread(Socket socket, String message) {
        this.socket = socket;
        this.message = message;
    }

    @Override
    public void run() {
        try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println(message); // Envoie le message
            System.out.println("Message envoyé : " + message);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi du message : " + e.getMessage());
        }
    }
}