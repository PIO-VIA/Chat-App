package org.personnal.client.database.DAO;

import org.personnal.client.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDAO {
    public static void adduser(String username, String email, String pass){
        String sql="insert into users(username, email, password_hash) values(?, ?, ?)";
        try (Connection conn=DatabaseConnection.connect();
             PreparedStatement stmt= conn.prepareStatement(sql)){
            stmt.setString(1,username);
            stmt.setString(2,email);
            stmt.setString(3,pass);
            stmt.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }
}