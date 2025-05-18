package org.personnal.client.database.DAO;

import org.personnal.client.database.DatabaseConnection;
import org.personnal.client.model.FileData;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileDAO implements IFileDAO {

    @Override
    public void saveFile(FileData file) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO files (sender, receiver, filename, filepath, timestamp, read) VALUES (?, ?, ?, ?, ?, ?)")) {

            stmt.setString(1, file.getSender());
            stmt.setString(2, file.getReceiver());
            stmt.setString(3, file.getFilename());
            stmt.setString(4, file.getFilepath());

            // Formatage de la date
            String timestamp = file.getTimestamp() != null
                    ? file.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            stmt.setString(5, timestamp);

            stmt.setBoolean(6, file.isRead());

            stmt.executeUpdate();
            System.out.println("Fichier sauvegardé: " + file.getFilename());
        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde du fichier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<FileData> getFilesWith(String username) {
        List<FileData> files = new ArrayList<>();

        // Correction de la requête SQL pour récupérer les fichiers échangés entre l'utilisateur courant et le contact
        String sql = "SELECT * FROM files WHERE (sender = ? OR receiver = ?) AND (sender = ? OR receiver = ?) ORDER BY timestamp ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Paramètres pour récupérer les fichiers échangés avec le contact spécifié
            stmt.setString(1, username);  // username peut être sender
            stmt.setString(2, username);  // username peut être receiver

            // Utilisateur connecté (pour filtrer la conversation)
            String currentUser = System.getProperty("current.user");
            stmt.setString(3, currentUser);  // currentUser peut être sender
            stmt.setString(4, currentUser);  // currentUser peut être receiver

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                FileData file = new FileData();
                file.setId(rs.getInt("id"));
                file.setSender(rs.getString("sender"));
                file.setReceiver(rs.getString("receiver"));
                file.setFilename(rs.getString("filename"));
                file.setFilepath(rs.getString("filepath"));

                // Conversion de la chaîne timestamp en LocalDateTime
                String timestampStr = rs.getString("timestamp");
                if (timestampStr != null && !timestampStr.isEmpty()) {
                    try {
                        LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        file.setTimestamp(timestamp);
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la conversion du timestamp: " + e.getMessage());
                        // Utiliser la date actuelle en cas d'erreur
                        file.setTimestamp(LocalDateTime.now());
                    }
                } else {
                    file.setTimestamp(LocalDateTime.now());
                }

                file.setRead(rs.getBoolean("read"));
                files.add(file);
            }

            System.out.println("Nombre de fichiers récupérés avec " + username + ": " + files.size());
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des fichiers: " + e.getMessage());
            e.printStackTrace();
        }

        return files;
    }

    @Override
    public void deleteFileById(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM files WHERE id = ?")) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("Fichier #" + id + " supprimé");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du fichier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Marque tous les fichiers d'un expéditeur comme lus
     * @param sender L'expéditeur des fichiers
     * @param receiver Le destinataire des fichiers
     */
    public void markFilesAsRead(String sender, String receiver) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE files SET read = 1 WHERE sender = ? AND receiver = ? AND read = 0")) {

            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println(rowsUpdated + " fichiers marqués comme lus");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du marquage des fichiers comme lus: " + e.getMessage());
            e.printStackTrace();
        }
    }
}