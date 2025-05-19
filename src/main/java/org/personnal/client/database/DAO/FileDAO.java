package org.personnal.client.database.DAO;

import org.personnal.client.database.DatabaseConnection;
import org.personnal.client.model.FileData;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

public class FileDAO implements IFileDAO {

    // Cache pour les résultats fréquemment consultés
    private final Map<String, CachedFileList> fileCache = new ConcurrentHashMap<>();

    // Délai d'expiration du cache en millisecondes (10 secondes)
    private static final long CACHE_VALIDITY_MS = 10000;

    @Override
    public void saveFile(FileData file) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO files (sender, receiver, filename, filepath, timestamp, read) VALUES (?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

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

            // Récupérer l'ID généré
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    file.setId(generatedKeys.getInt(1));
                }
            }

            // Invalider le cache pour ces utilisateurs
            invalidateCache(file.getSender(), file.getReceiver());

            System.out.println("Fichier sauvegardé: " + file.getFilename());
        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde du fichier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<FileData> getFilesWith(String username) {
        // Vérifier le cache d'abord
        String currentUser = System.getProperty("current.user");
        String cacheKey = getCacheKey(currentUser, username);

        CachedFileList cachedResult = fileCache.get(cacheKey);
        if (cachedResult != null && !cachedResult.isExpired()) {
            return new ArrayList<>(cachedResult.getFiles());
        }

        List<FileData> files = new ArrayList<>();

        // Correction de la requête SQL pour récupérer les fichiers échangés entre l'utilisateur courant et le contact
        String sql = "SELECT * FROM files WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY timestamp ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Paramètres pour récupérer les fichiers échangés avec le contact spécifié
            stmt.setString(1, username);
            stmt.setString(2, currentUser);
            stmt.setString(3, currentUser);
            stmt.setString(4, username);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FileData file = mapResultSetToFile(rs);
                    files.add(file);
                }
            }

            // Mettre en cache le résultat
            fileCache.put(cacheKey, new CachedFileList(files, System.currentTimeMillis() + CACHE_VALIDITY_MS));

            System.out.println("Nombre de fichiers récupérés avec " + username + ": " + files.size());
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des fichiers: " + e.getMessage());
            e.printStackTrace();
        }

        return files;
    }

    /**
     * Récupère un fichier par son ID
     */
    public FileData getFileById(int id) {
        String sql = "SELECT * FROM files WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFile(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du fichier: " + e.getMessage());
        }

        return null;
    }

    @Override
    public void deleteFileById(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM files WHERE id = ?")) {

            // Récupérer d'abord le fichier pour invalider le cache
            FileData file = getFileById(id);

            stmt.setInt(1, id);
            stmt.executeUpdate();

            // Invalider le cache si le fichier a été trouvé
            if (file != null) {
                invalidateCache(file.getSender(), file.getReceiver());
            }

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
                // Invalider le cache
                invalidateCache(sender, receiver);
                System.out.println(rowsUpdated + " fichiers marqués comme lus");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du marquage des fichiers comme lus: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Vérifie si l'utilisateur a des fichiers non lus d'un contact spécifique
     */
    public boolean hasUnreadFilesFrom(String sender) {
        String currentUser = System.getProperty("current.user");
        boolean hasUnread = false;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM files WHERE sender = ? AND receiver = ? AND read = 0")) {

            stmt.setString(1, sender);
            stmt.setString(2, currentUser);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    hasUnread = rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification des fichiers non lus: " + e.getMessage());
        }

        return hasUnread;
    }

    /**
     * Supprime les fichiers plus anciens qu'une certaine date
     * Utilisé pour le nettoyage périodique
     */
    public int deleteOldFiles(LocalDateTime olderThan) {
        String sql = "DELETE FROM files WHERE timestamp < ?";
        int deletedCount = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, olderThan.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            deletedCount = stmt.executeUpdate();

            // Vider tout le cache après une suppression massive
            if (deletedCount > 0) {
                fileCache.clear();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression des anciens fichiers: " + e.getMessage());
        }

        return deletedCount;
    }

    /**
     * Mappe un ResultSet à un objet FileData
     */
    private FileData mapResultSetToFile(ResultSet rs) throws SQLException {
        FileData file = new FileData();
        file.setId(rs.getInt("id"));
        file.setSender(rs.getString("sender"));
        file.setReceiver(rs.getString("receiver"));
        file.setFilename(rs.getString("filename"));
        file.setFilepath(rs.getString("filepath"));
        file.setRead(rs.getBoolean("read"));

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

        return file;
    }

    /**
     * Invalide le cache pour une conversation entre deux utilisateurs
     */
    private void invalidateCache(String user1, String user2) {
        String cacheKey = getCacheKey(user1, user2);
        fileCache.remove(cacheKey);
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
     * Nettoie les entrées expirées du cache
     */
    public void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        fileCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    /**
     * Classe interne pour stocker les fichiers en cache avec une date d'expiration
     */
    private static class CachedFileList {
        private final List<FileData> files;
        private final long expiryTime;

        public CachedFileList(List<FileData> files, long expiryTime) {
            this.files = new ArrayList<>(files);
            this.expiryTime = expiryTime;
        }

        public List<FileData> getFiles() {
            return files;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }

        public boolean isExpired(long now) {
            return now > expiryTime;
        }
    }
}