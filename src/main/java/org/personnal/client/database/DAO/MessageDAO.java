package org.personnal.client.database.DAO;

import org.personnal.client.database.DatabaseConnection;
import org.personnal.client.model.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageDAO implements IMessageDAO {
    // Cache des messages récents pour éviter des requêtes répétées
    private final Map<String, CachedMessages> messageCache = new ConcurrentHashMap<>();

    // Délai de validité du cache (5 secondes)
    private static final long CACHE_VALIDITY_MS = 5000;

    @Override
    public void saveMessage(Message msg) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO messages (sender, receiver, content, timestamp, read) VALUES (?, ?, ?, ?, ?)")) {

            stmt.setString(1, msg.getSender());
            stmt.setString(2, msg.getReceiver());
            stmt.setString(3, msg.getContent());

            // Formatage de la date
            String timestamp = msg.getTimestamp() != null
                    ? msg.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            stmt.setString(4, timestamp);

            stmt.setBoolean(5, msg.isRead());

            stmt.executeUpdate();

            // Invalider le cache pour cette conversation
            String cacheKey = getCacheKey(msg.getSender(), msg.getReceiver());
            messageCache.remove(cacheKey);
            cacheKey = getCacheKey(msg.getReceiver(), msg.getSender());
            messageCache.remove(cacheKey);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde du message : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Message> getMessagesWith(String username) {
        // Vérifier le cache d'abord
        String currentUser = System.getProperty("current.user");
        String cacheKey = getCacheKey(currentUser, username);

        CachedMessages cachedResult = messageCache.get(cacheKey);
        if (cachedResult != null && !cachedResult.isExpired()) {
            return new ArrayList<>(cachedResult.getMessages());
        }

        // Sinon, exécuter la requête
        List<Message> messages = new ArrayList<>();

        // Requête optimisée avec index sur sender et receiver
        String sql = "SELECT * FROM messages WHERE " +
                "((sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)) " +
                "ORDER BY timestamp ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, currentUser);
            stmt.setString(3, currentUser);
            stmt.setString(4, username);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Message msg = new Message();
                    msg.setIdMessage(rs.getInt("id"));
                    msg.setSender(rs.getString("sender"));
                    msg.setReceiver(rs.getString("receiver"));
                    msg.setContent(rs.getString("content"));

                    // Conversion de la chaîne timestamp en LocalDateTime
                    String timestampStr = rs.getString("timestamp");
                    if (timestampStr != null && !timestampStr.isEmpty()) {
                        try {
                            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            msg.setTimestamp(timestamp);
                        } catch (Exception e) {
                            msg.setTimestamp(LocalDateTime.now());
                        }
                    } else {
                        msg.setTimestamp(LocalDateTime.now());
                    }

                    msg.setRead(rs.getBoolean("read"));
                    messages.add(msg);
                }
            }

            // Mettre en cache le résultat
            messageCache.put(cacheKey, new CachedMessages(messages, System.currentTimeMillis() + CACHE_VALIDITY_MS));

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des messages: " + e.getMessage());
            e.printStackTrace();
        }

        return messages;
    }

    @Override
    public void deleteMessageById(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM messages WHERE id = ?")) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

            // Invalider tous les caches car nous ne savons pas quelle conversation est affectée
            messageCache.clear();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Marque tous les messages d'un expéditeur comme lus
     * @param sender L'expéditeur des messages
     * @param receiver Le destinataire des messages
     */
    @Override
    public void markMessagesAsRead(String sender, String receiver) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE messages SET read = 1 WHERE sender = ? AND receiver = ? AND read = 0")) {

            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                // Invalider le cache pour cette conversation
                String cacheKey = getCacheKey(sender, receiver);
                messageCache.remove(cacheKey);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du marquage des messages comme lus: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Vérifie si l'utilisateur a des messages non lus d'un contact spécifique
     * @param sender L'expéditeur des messages
     * @return true si des messages non lus existent, false sinon
     */
    @Override
    public boolean hasUnreadMessagesFrom(String sender) {
        boolean hasUnread = false;
        String currentUser = System.getProperty("current.user");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM messages WHERE sender = ? AND receiver = ? AND read = 0")) {

            stmt.setString(1, sender);
            stmt.setString(2, currentUser);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    hasUnread = rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification des messages non lus: " + e.getMessage());
            e.printStackTrace();
        }

        return hasUnread;
    }

    /**
     * Obtient la clé de cache pour une conversation entre deux utilisateurs
     */
    private String getCacheKey(String user1, String user2) {
        // Ordonner alphabétiquement pour avoir la même clé quelle que soit la direction
        if (user1.compareTo(user2) < 0) {
            return user1 + ":" + user2;
        } else {
            return user2 + ":" + user1;
        }
    }

    /**
     * Classe interne pour stocker les messages en cache avec une date d'expiration
     */
    private static class CachedMessages {
        private final List<Message> messages;
        private final long expiryTime;

        public CachedMessages(List<Message> messages, long expiryTime) {
            this.messages = new ArrayList<>(messages);
            this.expiryTime = expiryTime;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}