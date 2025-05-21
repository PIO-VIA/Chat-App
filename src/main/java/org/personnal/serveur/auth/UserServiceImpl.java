package org.personnal.serveur.auth;


import org.personnal.serveur.database.DAO.IUserDAO;
import org.personnal.serveur.database.DAO.UserDAO;
import org.personnal.serveur.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserServiceImpl implements IUserService {
    private final IUserDAO userDao;
    private final PasswordHasher passwordHasher;

    public UserServiceImpl() {
        this.userDao = new UserDAO();
        this.passwordHasher = new PasswordHasher();
    }

    @Override
    public User login(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user == null) {
            return null; // Utilisateur non trouvé
        }

        if (passwordHasher.checkPassword(password, user.getPassword())) {
            return user; // Authentification réussie
        }
        return null; // Mot de passe incorrect
    }

    @Override
    public User register(String username,String email, String password) {
        if (userDao.usernameExists(username)) {
            return null; // Username déjà pris
        }

        String hashedPassword = passwordHasher.hashPassword(password);
        User newUser = new User(username,email, hashedPassword);

        if (userDao.save(newUser)) {
            return newUser;
        }
        return null; // Échec de l'enregistrement
    }

    // Ajouter à la classe
    private final Map<String, Boolean> userExistsCache = new ConcurrentHashMap<>();
    private final long USER_CACHE_EXPIRY = 3600000; // 1 heure
    private final Map<String, Long> userCacheTimestamps = new ConcurrentHashMap<>();

    @Override
    public boolean userExists(String username) {
        // Vérifier le cache d'abord
        if (userExistsCache.containsKey(username)) {
            Long timestamp = userCacheTimestamps.get(username);
            if (timestamp != null && System.currentTimeMillis() - timestamp < USER_CACHE_EXPIRY) {
                return userExistsCache.get(username);
            }
        }

        // Optimisation: utiliser directement usernameExists au lieu de findByUsername
        boolean exists = userDao.usernameExists(username);

        // Mettre en cache
        userExistsCache.put(username, exists);
        userCacheTimestamps.put(username, System.currentTimeMillis());

        return exists;
    }
}