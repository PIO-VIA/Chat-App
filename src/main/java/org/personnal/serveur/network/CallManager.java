package org.personnal.serveur.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CallManager {

    // Structure d'une session d'appel
    private static class CallSession {
        private final String caller;
        private final String callee;
        private CallStatus status;
        private final long creationTime;

        public CallSession(String caller, String callee) {
            this.caller = caller;
            this.callee = callee;
            this.status = CallStatus.RINGING;
            this.creationTime = System.currentTimeMillis();
        }
    }

    // Statuts possibles d'un appel
    public enum CallStatus {
        RINGING,       // En attente de réponse
        CONNECTED,     // Appel en cours
        REJECTED,      // Rejeté
        ENDED          // Terminé
    }

    // Map des sessions d'appel actives (clé: caller_callee)
    private static final Map<String, CallSession> activeCalls = new ConcurrentHashMap<>();

    /**
     * Crée une nouvelle session d'appel
     * @param caller Nom de l'appelant
     * @param callee Nom de l'appelé
     * @return true si l'appel a été créé, false sinon
     */
    public static boolean createCall(String caller, String callee) {
        String callId = getCallId(caller, callee);

        // Vérifier qu'aucun appel n'est en cours entre ces utilisateurs
        if (activeCalls.containsKey(callId)) {
            CallSession existingCall = activeCalls.get(callId);
            if (existingCall.status == CallStatus.RINGING || existingCall.status == CallStatus.CONNECTED) {
                return false; // Appel déjà en cours
            }
        }

        // Créer la nouvelle session
        CallSession newCall = new CallSession(caller, callee);
        activeCalls.put(callId, newCall);
        System.out.println("✅ Nouvel appel créé: " + caller + " -> " + callee);
        return true;
    }

    /**
     * Accepte un appel en attente
     * @param caller Nom de l'appelant
     * @param callee Nom de l'appelé
     * @return true si l'appel a été accepté, false sinon
     */
    public static boolean acceptCall(String caller, String callee) {
        String callId = getCallId(caller, callee);
        CallSession call = activeCalls.get(callId);

        if (call != null && call.status == CallStatus.RINGING) {
            call.status = CallStatus.CONNECTED;
            System.out.println("✅ Appel accepté: " + caller + " -> " + callee);
            return true;
        }
        return false;
    }

    /**
     * Rejette un appel en attente
     * @param caller Nom de l'appelant
     * @param callee Nom de l'appelé
     * @return true si l'appel a été rejeté, false sinon
     */
    public static boolean rejectCall(String caller, String callee) {
        String callId = getCallId(caller, callee);
        CallSession call = activeCalls.get(callId);

        if (call != null && call.status == CallStatus.RINGING) {
            call.status = CallStatus.REJECTED;
            System.out.println("❌ Appel rejeté: " + caller + " -> " + callee);
            // Planifier la suppression de l'appel
            removeCallLater(callId);
            return true;
        }
        return false;
    }

    /**
     * Termine un appel en cours
     * @param caller Nom de l'appelant
     * @param callee Nom de l'appelé
     * @return true si l'appel a été terminé, false sinon
     */
    public static boolean endCall(String caller, String callee) {
        String callId = getCallId(caller, callee);
        CallSession call = activeCalls.get(callId);

        if (call != null) {
            call.status = CallStatus.ENDED;
            System.out.println("🛑 Appel terminé: " + caller + " -> " + callee);
            // Planifier la suppression de l'appel
            removeCallLater(callId);
            return true;
        }
        return false;
    }

    /**
     * Vérifie si un appel est actif entre deux utilisateurs
     * @param user1 Premier utilisateur
     * @param user2 Second utilisateur
     * @return true si un appel est actif, false sinon
     */
    public static boolean isCallActive(String user1, String user2) {
        String callId = getCallId(user1, user2);
        CallSession call = activeCalls.get(callId);
        return call != null && (call.status == CallStatus.RINGING || call.status == CallStatus.CONNECTED);
    }

    /**
     * Nettoie les appels terminés ou rejetés après un délai
     * @param callId Identifiant de l'appel
     */
    private static void removeCallLater(String callId) {
        new Thread(() -> {
            try {
                // Attendre 30 secondes avant de supprimer
                Thread.sleep(30000);
                activeCalls.remove(callId);
                System.out.println("🧹 Session d'appel nettoyée: " + callId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Génère un identifiant unique pour un appel entre deux utilisateurs
     * @param user1 Premier utilisateur
     * @param user2 Second utilisateur
     * @return Identifiant d'appel normalisé (toujours dans le même ordre)
     */
    private static String getCallId(String user1, String user2) {
        // Assurer que l'ordre des utilisateurs est toujours le même pour le même appel
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    /**
     * Nettoie tous les appels lors de l'arrêt du serveur
     */
    public static void shutdown() {
        activeCalls.clear();
        System.out.println("🧹 Toutes les sessions d'appel ont été nettoyées");
    }
}