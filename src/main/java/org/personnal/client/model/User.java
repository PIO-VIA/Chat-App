package org.personnal.client.model;

import java.io.Serializable;

public class User implements Serializable {
    private int idUser;
    private String username;
    private String status;
    private String email;


    public User() {}

    public User(int idUser, String username,String email, String status) {
        this.idUser = idUser;
        this.username = username;
        this.status = status;
        this.email = email;

    }
    public User(int idUser, String username,String email) {
        this.idUser = idUser;
        this.username = username;
        this.email = email;

    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getEmail() {return email;}
    public int getIdUser() { return idUser; }
    public String getUsername() { return username; }
    public String getStatus() { return status; }

    public void setIdUser(int id) { this.idUser = id; }
    public void setUsername(String username) { this.username = username; }
    public void setStatus(String status) { this.status = status; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + idUser +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
