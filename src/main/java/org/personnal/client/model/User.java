/**
 * Classe User
 * Utilisation du pattern State pour gérer les états
 * Utilisation du pattern Observer pour les notifications
 * Utilisation du pattern Builder pour la construction complexe
 */

package org.personnal.client.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

public class User {
    private String id;
    private String email;
    private String username;
    private String hashedPassword;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private String avatarUrl;
    private IUserStatus state;
    private List<IUserObserver> observers;

    /**
     * Constructeur privé pour forcer l'utilisation du builder
     */
    private User() {
        this.createdAt = LocalDateTime.now();
        this.lastLogin = LocalDateTime.now();
        this.state = UserStatus.OFFLINE; // État par défaut
        this.observers = new ArrayList<>();
    }

    /**
     * Crée un UserBuilder pour construire l'utilisateur (Pattern Builder)
     */
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    /**
     * Getters et setters avec validation pour assurer l'intégrité des données
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Email invalide");
        }
        this.email = email;
    }

    private boolean isValidEmail(String email) {
        String regex =
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email != null && email.matches(regex);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.length() < 3) {
            throw new IllegalArgumentException("Le nom d'utilisateur doit contenir au moins 3 caractères");
        }
        this.username = username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    /**
     * Ne stocke jamais le mot de passe en clair
     */
    public void setPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères");
        }
        this.hashedPassword = hashPassword(password);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur lors du hashage du mot de passe", e);
        }
    }

    public boolean verifyPassword(String password) {
        return hashPassword(password).equals(hashedPassword);
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public IUserStatus getState() {
        return state;
    }

    public void setStatus(UserStatus newStatus) {
        String oldLabel = (state == null ? null : state.getStatusLabel());
        this.state = newStatus;
        notifyStatusChanged(oldLabel, newStatus.getStatusLabel());
    }

    public boolean canSendMessage() {
        return this.state.canSendMessage();
    }

    public boolean canReceiveMessage() {
        return this.state.canReceiveMessage();
    }

    /**
     * Implémentation du pattern Observer
     */
    public void attachObserver(IUserObserver observer) {
        this.observers.add(observer);
    }

    public void detachObserver(IUserObserver observer) {
        this.observers.remove(observer);
    }

    private void notifyStatusChanged(String oldStatus, String newStatus) {
        for (IUserObserver observer : observers) {
            observer.onUserStatusChanged(this, oldStatus, newStatus);
        }
    }

    private void notifyProfileUpdated() {
        for (IUserObserver observer : observers) {
            observer.onUserProfileUpdated(this);
        }
    }

    /**
     * Méthode toMap pour transformer l'objet en Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("email", email);
        map.put("username", username);
        map.put("avatarUrl", avatarUrl);
        map.put("status", state.getStatusLabel());
        map.put("lastLogin", lastLogin.toString());
        map.put("createdAt", createdAt.toString());
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", status=" + state.getStatusLabel() +
                '}';
    }

    /**
     * Builder pour la création d'objets User complexes (Pattern Builder)
     */
    public static class UserBuilder {
        private final User user;

        public UserBuilder() {
            this.user = new User();
        }

        public UserBuilder withId(String id) {
            user.setId(id);
            return this;
        }

        public UserBuilder withEmail(String email) {
            user.setEmail(email);
            return this;
        }

        public UserBuilder withUsername(String username) {
            user.setUsername(username);
            return this;
        }

        public UserBuilder withPassword(String password) {
            user.setPassword(password);
            return this;
        }

        public UserBuilder withAvatarUrl(String avatarUrl) {
            user.setAvatarUrl(avatarUrl);
            return this;
        }

        public UserBuilder withStatus(UserStatus status) {
            user.setStatus(status);
            return this;
        }

        public User build() {
            return user;
        }
    }


}

