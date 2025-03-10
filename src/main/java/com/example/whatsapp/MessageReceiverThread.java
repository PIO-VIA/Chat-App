package com.example.whatsapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MessageReceiverThread extends Thread {
    private Socket socket;

    public MessageReceiverThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while ((message = reader.readLine()) != null) { // Lit les messages
                System.out.println("Message reçu : " + message);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la réception du message : " + e.getMessage());
        }
    }
}