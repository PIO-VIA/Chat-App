package org.personnal.client.database.DAO;


import java.util.List;

public interface BaseDAO<T> {
    void insert(T t);
    void update(T t);
    void delete(int id);
    T findById(int id);
    List<T> findAll();
}
