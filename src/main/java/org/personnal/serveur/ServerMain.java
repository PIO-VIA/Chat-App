package org.personnal.serveur;

import org.personnal.serveur.network.ServerSocketManager;

public class ServerMain {
    public static void main(String[] args) {
        ServerSocketManager server = new ServerSocketManager();
        server.addShutdownHook();  // Ajouter un hook d'arrêt pour la fermeture propre
        server.start();
    }
}