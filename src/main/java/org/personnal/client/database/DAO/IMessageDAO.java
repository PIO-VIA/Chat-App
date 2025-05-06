package org.personnal.client.database.DAO;

public interface IMessageDAO {
    void sendmess(int senderId, int receivedId, String content);
    void showUserMessages(int userId);
    String getusername(int userId);
}