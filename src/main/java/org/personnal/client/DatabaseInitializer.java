package org.personnal.client;


import org.personnal.client.database.DatabaseConnection;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe utilitaire pour initialiser la base de données
 */
public class DatabaseInitializer {

    /**
     * Initialise la base de données si nécessaire
     */
    public static void initialize() {
        try {
            // Vérifier si le fichier de la base de données existe
            File dbFile = new File("client_chat.db");
            boolean isNew = !dbFile.exists();

            // Obtenir une connexion (cela créera la BD si elle n'existe pas)
            Connection conn = DatabaseConnection.getConnection();

            // Si c'est une nouvelle BD ou pour s'assurer que toutes les tables existent
            createTables(conn);

            if (isNew) {
                System.out.println("Base de données initialisée avec succès");
            } else {
                System.out.println("Connexion à la base de données réussie");
            }

            conn.close();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'initialisation de la base de données: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crée les tables dans la base de données
     */
    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Table pour stocker les contacts de l'utilisateur
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "username TEXT NOT NULL UNIQUE, " +
                            "email TEXT, " +
                            "status TEXT" +
                            ")"
            );

            // Table pour stocker les messages
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS messages (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "sender TEXT NOT NULL, " +
                            "receiver TEXT NOT NULL, " +
                            "content TEXT NOT NULL, " +
                            "timestamp TEXT DEFAULT CURRENT_TIMESTAMP, " +
                            "read BOOLEAN NOT NULL DEFAULT 0, " +
                            "is_sent_by_me BOOLEAN DEFAULT 0" +
                            ")"
            );

            // Table pour stocker les fichiers
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS files (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "sender TEXT NOT NULL, " +
                            "receiver TEXT NOT NULL, " +
                            "filename TEXT NOT NULL, " +
                            "filepath TEXT, " +
                            "timestamp TEXT DEFAULT CURRENT_TIMESTAMP, " +
                            "read BOOLEAN NOT NULL DEFAULT 0, " +
                            "is_sent_by_me BOOLEAN DEFAULT 0" +
                            ")"
            );

            // Créer des index pour optimiser les recherches
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages(receiver)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_files_sender ON files(sender)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_files_receiver ON files(receiver)");
        }
    }
}