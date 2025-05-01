package org.personnal.client.model;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String username;
    private String email;
    private String password;
    private String registered_at;
    private String profil;

    public User() {}

    public User(int id, String username, String email, String password, String registered_at, String profil) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.registered_at = registered_at;
        this.profil = profil;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }


    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRegistered_at() { return registered_at; }
    public String getProfil() { return profil; }

    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRegistered_at(String registered_at) { this.registered_at = registered_at; }
    public void setProfil(String profil) { this.profil = profil; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", registered_at='" + registered_at + '\'' +
                '}';
    }
}
