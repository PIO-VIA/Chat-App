package org.personnal.serveur.database.DAO;

import org.personnal.serveur.database.DatabaseConnection;
import org.personnal.serveur.model.User;

import java.sql.*;

public class UserDAO implements IUserDAO {

    @Override
    public User findByUsername(String username) {
        String query = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("registered_at"),
                            rs.getString("profil")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur recherche utilisateur: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean save(User user) {
        String query = "INSERT INTO users(username, password_hash) VALUES(?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.executeUpdate(); // ✅ OK pour INSERT
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la sauvegarde de l'utilisateur: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean usernameExists(String username) {
        String query = "SELECT 1 FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            try (ResultSet res = pstmt.executeQuery()) {
                return res.next(); // ✅ OK
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la vérification du username: " + e.getMessage());
            return false;
        }
    }
}
