package org.personnal.client.database.DAO;


import org.personnal.client.database.DatabaseConnection;
import org.personnal.client.model.FileData;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileDAO implements IFileDAO {
    Connection conn;

    {
        try {
            conn = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveFile(FileData file) {
        String sql = "INSERT INTO files (sender, receiver, filename, filepath, timestamp, is_sent_by_me) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, file.getSender());
            stmt.setString(2, file.getReceiver());
            stmt.setString(3, file.getFilename());
            stmt.setString(4, file.getFilepath());
            stmt.setString(5, file.getTimestamp().toString());
            stmt.setBoolean(6, file.isRead());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<FileData> getFilesWith(String username) {
        String sql = "SELECT * FROM files WHERE sender = ? OR receiver = ? ORDER BY timestamp ASC";
        List<FileData> files = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                FileData file = new FileData();
                file.setId(rs.getInt("id"));
                file.setSender(rs.getString("sender"));
                file.setReceiver(rs.getString("receiver"));
                file.setFilename(rs.getString("filename"));
                file.setFilepath(rs.getString("filepath"));
                file.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
                file.setRead(rs.getBoolean("is_sent_by_me"));
                files.add(file);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return files;
    }

    @Override
    public void deleteFileById(int id) {
        String sql = "DELETE FROM files WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
