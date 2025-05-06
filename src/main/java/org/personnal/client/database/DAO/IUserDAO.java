package org.personnal.client.database.DAO;

import org.personnal.client.model.user.User;

public interface IUserDAO {
    void adduser(String username, String email, String pass);

   /* User findByUserName(String username);

    void save(User user);

    void delete(User user);

    User findByEmail(String email);
    */

}
