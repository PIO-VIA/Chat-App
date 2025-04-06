package org.personnal.serveur.database.DAO;


import org.personnal.serveur.model.User;

public interface IUserDAO {
    /**
     * Trouve un utilisateur par son nom d'utilisateur
     * @param username Le nom d'utilisateur à rechercher
     * @return L'utilisateur trouvé ou null si non existant
     */
    User findByUsername(String username);

    /**
     * Sauvegarde un nouvel utilisateur en base de données
     * @param user L'utilisateur à sauvegarder
     * @return true si l'opération a réussi, false sinon
     */
    boolean save(User user);

    /**
     * Vérifie si un nom d'utilisateur existe déjà
     * @param username Le nom d'utilisateur à vérifier
     * @return true si l'utilisateur existe déjà
     */
    boolean usernameExists(String username);
}
