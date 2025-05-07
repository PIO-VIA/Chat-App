package org.personnal.client.model;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String username;
    private String status;
    private String profil;

    public User() {}

    public User(int id, String username, String status,  String profil) {
        this.id = id;
        this.username = username;
        this.status = status;
        this.profil = profil;
    }

    public User(String username, String status) {
        this.username = username;
        this.status = status;
    }


    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getStatus() { return status; }

    public String getProfil() { return profil; }

    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setStatus(String status) { this.status = status; }
    public void setProfil(String profil) { this.profil = profil; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
