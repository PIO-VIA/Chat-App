package org.personnal.serveur.network;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    // Utiliser ConcurrentHashMap pour la thread-safety
    private static final Map<String, ClientHandler> sessions = new ConcurrentHashMap<>();

    // Timeout d'inactivité en millisecondes (5 minutes)
    private static final long INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000;

    // Intervalle de vérification des sessions inactives (1 minute)
    private static final long CHECK_INTERVAL_SECONDS = 60;

    // Scheduler pour les tâches périodiques
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    static {
        // Démarrer la tâche périodique de nettoyage des sessions inactives
        scheduler.scheduleAtFixedRate(
                SessionManager::cleanInactiveSessions,
                CHECK_INTERVAL_SECONDS,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * Ajoute un utilisateur à la liste des sessions actives
     */
    public static void addUser(String username, ClientHandler handler) {
        sessions.put(username, handler);
        System.out.println("Utilisateur ajouté: " + username + ". Total: " + sessions.size());
    }

    /**
     * Supprime un utilisateur de la liste des sessions actives
     */
    public static void removeUser(String username) {
        sessions.remove(username);
        System.out.println("Utilisateur supprimé: " + username + ". Total: " + sessions.size());
    }

    /**
     * Récupère le handler d'un utilisateur
     */
    public static ClientHandler getUserHandler(String username) {
        return sessions.get(username);
    }

    /**
     * Récupère toutes les sessions actives
     */
    public static Map<String, ClientHandler> getAllSessions() {
        return sessions;
    }

    /**
     * Vérifie si un utilisateur est en ligne
     */
    public static boolean isUserOnline(String username) {
        return sessions.containsKey(username);
    }

    /**
     * Nettoie les sessions inactives
     */
    public static void cleanInactiveSessions() {
        try {
            int beforeCount = sessions.size();
            long now = System.currentTimeMillis();

            // Identifier et supprimer les sessions inactives
            Set<String> usersToRemove = sessions.entrySet().stream()
                    .filter(entry -> entry.getValue().isInactive(INACTIVITY_TIMEOUT_MS))
                    .map(Map.Entry::getKey)
                    .collect(java.util.stream.Collectors.toSet());

            // Supprimer les sessions inactives
            for (String user : usersToRemove) {
                sessions.remove(user);
                System.out.println("Session inactive supprimée pour: " + user);
            }

            int afterCount = sessions.size();
            if (beforeCount != afterCount) {
                System.out.println("Nettoyage des sessions terminé. Avant: " + beforeCount + ", Après: " + afterCount);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du nettoyage des sessions inactives: " + e.getMessage());
        }
    }

    /**
     * Arrête le scheduler (à appeler lors de l'arrêt du serveur)
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}