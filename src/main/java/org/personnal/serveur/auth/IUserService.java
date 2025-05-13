package org.personnal.serveur.auth;


import org.personnal.serveur.model.User;

public interface IUserService {
    /**
     * Tentative de connexion d'un utilisateur
     * @param username Nom d'utilisateur
     * @param password Mot de passe en clair
     * @return User si authentification réussie, null sinon
     */
    User login(String username, String password);

    /**
     * Enregistrement d'un nouvel utilisateur
     * @param username Nom d'utilisateur
     * @param password Mot de passe en clair
     * @return User créé si succès, null si échec (username déjà pris)
     */
    User register(String username,String email, String password);
}