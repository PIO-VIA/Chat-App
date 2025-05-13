package org.personnal.serveur.network;


import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SessionManager {
    private static final Map<String, ClientHandler> sessions = new HashMap<>();

    public static void addUser(String username, ClientHandler handler) {
        sessions.put(username, handler);
        System.out.println("Utilisateur ajouté: " + username + ". Total: " + sessions.size());
    }

    public static void removeUser(String username) {
        sessions.remove(username);
        System.out.println("Utilisateur supprimé: " + username + ". Total: " + sessions.size());
    }

    public static ClientHandler getUserHandler(String username) {
        return sessions.get(username);
    }

    public static Map<String, ClientHandler> getAllSessions() {
        return sessions;
    }
    public static boolean isUserOnline(String username) {
        return sessions.containsKey(username);
    }


}
