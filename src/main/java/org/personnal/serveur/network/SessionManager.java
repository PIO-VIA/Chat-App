package org.personnal.serveur.network;


import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SessionManager {
    private static final Map<String, ClientHandler> sessions = new ConcurrentHashMap<>();

    public static void addUser(String username, ClientHandler handler) {
        sessions.put(username, handler);
    }

    public static void removeUser(String username) {
        sessions.remove(username);
    }

    public static ClientHandler getUserHandler(String username) {
        return sessions.get(username);
    }

    public static boolean isUserOnline(String username) {
        return sessions.containsKey(username);
    }

    public static Map<String, ClientHandler> getAllSessions() {
        return sessions;
    }
}
