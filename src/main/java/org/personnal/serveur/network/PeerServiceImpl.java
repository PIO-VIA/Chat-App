package org.personnal.serveur.network;


import org.personnal.serveur.network.SessionManager;
import org.personnal.serveur.network.ClientHandler;

public class PeerServiceImpl {

    public boolean sendMessageToUser(String receiverUsername, String message, String fromUsername) {
        ClientHandler receiver = SessionManager.getUserHandler(receiverUsername);

        if (receiver != null) {
            receiver.sendTextMessage(fromUsername, message);
            return true;
        }

        return false;
    }

    public boolean isUserConnected(String username) {
        return SessionManager.isUserOnline(username);
    }
}
