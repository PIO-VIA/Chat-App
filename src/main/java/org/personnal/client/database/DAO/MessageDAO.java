package org.personnal.client.database.DAO;
import org.personnal.client.database.DatabaseConnection;

import java.sql.*;

public class MessageDAO implements IMessageDAO{
    @Override
    public void sendmess(int senderId, int receivedId, String content){
        String sql="insert into messages(sender_id, received_id, content) values (?, ?, ?)";
        try(Connection conn= DatabaseConnection.connect();
            PreparedStatement stmt= conn.prepareStatement(sql)){
            stmt.setInt(1, senderId);
            stmt.setInt(2, receivedId);
            stmt.setString(3, content);
            stmt.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }
    @Override
    public void showUserMessages(int userId) {
        String sql = "SELECT sender_id, content FROM messages WHERE received_id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n=== Vos messages ===");
            while (rs.next()) {
                int senderId = rs.getInt("sender_id");
                String content = rs.getString("content");
                //String timestamp = rs.getString("lecture");
                String nom=getusername(senderId);
                System.out.println("📩 De l'utilisateur " + nom );
                System.out.println("   ➜ " + content);
                System.out.println("--------------------------------");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @Override
    public String getusername(int userId){
        try (var conn = DatabaseConnection.connect();
             var stmt = conn.prepareStatement("SELECT username FROM users WHERE id = ?")) {
            stmt.setInt(1, userId);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return " ";
    }
}
