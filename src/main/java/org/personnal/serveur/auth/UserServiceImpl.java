package org.personnal.serveur.auth;


import org.personnal.serveur.database.DAO.IUserDAO;
import org.personnal.serveur.database.DAO.UserDAO;
import org.personnal.serveur.model.User;

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
}