package org.personnal.client.database.DAO;

import org.personnal.client.model.User;

public interface IUserDAO {
    User findByUserName(String username);
    void save(User user);
    void delete(User user);
    User findByEmail(String email);
}
