package org.personnal.client.model;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String username;
    private String status;
    private String email;


    public User() {}

    public User(int id, String username,String email, String status) {
        this.id = id;
        this.username = username;
        this.status = status;
        this.email = email;

    }
    public User(int id, String username,String email) {
        this.id = id;
        this.username = username;
        this.email = email;

    }



    public String getEmail() {return email;}
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getStatus() { return status; }

    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setStatus(String status) { this.status = status; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
