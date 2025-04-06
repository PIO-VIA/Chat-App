package org.personnal.serveur.auth;



import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    // Coût du hachage (10-12 est un bon compromis sécurité/performance)
    private static final int HASH_ROUNDS = 12;

    /**
     * Hash un mot de passe en clair
     * @param plainPassword Mot de passe en clair
     * @return Mot de passe hashé
     */
    public String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(HASH_ROUNDS));
    }

    /**
     * Vérifie un mot de passe contre un hash
     * @param plainPassword Mot de passe en clair
     * @param hashedPassword Mot de passe hashé
     * @return true si correspondance
     */
    public boolean checkPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Cas où le hash est malformé
            return false;
        }
    }
}