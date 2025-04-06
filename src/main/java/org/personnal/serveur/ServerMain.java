package org.personnal.serveur;


import org.personnal.serveur.network.ServerSocketManager;

public class ServerMain {
    public static void main(String[] args) {
        new ServerSocketManager().start();
    }
}
