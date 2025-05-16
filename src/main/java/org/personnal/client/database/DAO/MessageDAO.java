package org.personnal.client.database.DAO;


import org.personnal.client.database.DatabaseConnection;
import org.personnal.client.model.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO implements IMessageDAO {
    Connection conn;

    {
        try {
            conn = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveMessage(Message msg) {
        String sql = "INSERT INTO messages (sender, receiver, content, timestamp, is_sent_by_me) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msg.getSender());
            stmt.setString(2, msg.getReceiver());
            stmt.setString(3, msg.getContent());
            stmt.setString(4, msg.getTimestamp().toString());
            stmt.setBoolean(5, msg.isSentByMe());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Message> getMessagesWith(String username) {
        String sql = "SELECT * FROM messages WHERE sender = ? OR receiver = ? ORDER BY timestamp ASC";
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message msg = new Message();
                msg.setId(rs.getInt("id"));
                msg.setSender(rs.getString("sender"));
                msg.setReceiver(rs.getString("receiver"));
                msg.setContent(rs.getString("content"));
                msg.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
                msg.setSentByMe(rs.getBoolean("is_sent_by_me"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public void deleteMessageById(int id) {
        String sql = "DELETE FROM messages WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

