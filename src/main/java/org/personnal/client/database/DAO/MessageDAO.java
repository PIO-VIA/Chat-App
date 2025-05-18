package org.personnal.client.database.DAO;

import org.personnal.client.database.DatabaseConnection;
import org.personnal.client.model.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO implements IMessageDAO {

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
            System.out.println("Message sauvegardé : " + msg.getContent() + " (de " + msg.getSender() + " à " + msg.getReceiver() + ")");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde du message : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Message> getMessagesWith(String username) {
        List<Message> messages = new ArrayList<>();

        // Correction de la requête SQL pour récupérer les messages échangés entre l'utilisateur courant et le contact
        String sql = "SELECT * FROM messages WHERE (sender = ? OR receiver = ?) AND (sender = ? OR receiver = ?) ORDER BY timestamp ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Paramètres pour récupérer les messages échangés avec le contact spécifié
            stmt.setString(1, username);  // username peut être sender
            stmt.setString(2, username);  // username peut être receiver

            // Utilisateur connecté (pour filtrer la conversation)
            // Dans cet exemple, nous récupérons tous les messages où soit l'utilisateur connecté,
            // soit le contact est impliqué
            String currentUser = System.getProperty("current.user");
            stmt.setString(3, currentUser);  // currentUser peut être sender
            stmt.setString(4, currentUser);  // currentUser peut être receiver

            ResultSet rs = stmt.executeQuery();

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
                        System.err.println("Erreur lors de la conversion du timestamp: " + e.getMessage());
                        // Utiliser la date actuelle en cas d'erreur
                        msg.setTimestamp(LocalDateTime.now());
                    }
                } else {
                    msg.setTimestamp(LocalDateTime.now());
                }

                msg.setRead(rs.getBoolean("read"));
                messages.add(msg);
            }

            System.out.println("Nombre de messages récupérés avec " + username + ": " + messages.size());
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
    public void markMessagesAsRead(String sender, String receiver) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE messages SET read = 1 WHERE sender = ? AND receiver = ? AND read = 0")) {

            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            int rowsUpdated = stmt.executeUpdate();

            System.out.println(rowsUpdated + " messages marqués comme lus");
        } catch (SQLException e) {
            System.err.println("Erreur lors du marquage des messages comme lus: " + e.getMessage());
            e.printStackTrace();
        }
    }
}