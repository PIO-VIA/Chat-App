package org.personnal.client.database.DAO;

import org.personnal.client.model.User;

public interface IUserDAO {
    void adduser(String username, String email, String pass);
}
