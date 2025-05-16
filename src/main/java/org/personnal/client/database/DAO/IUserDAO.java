package org.personnal.client.database.DAO;


import org.personnal.client.model.User;

public interface IUserDAO extends BaseDAO<User> {
    User findByUsername(String username);
}