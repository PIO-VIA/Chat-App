package org.personnal.client.database.DAO;

public interface IMessageDAO {
    void sendmess(int senderId, int receivedId, String content);
    void showUserMessages(int userId);

    String getusername(int userId);
    /*Message findById(String id);
    List<Message> findByConversationId(String conversationId);
    List<Message> findBySenderId(String senderId);
    void save(Message message);
    void delete(Message message);

     */
}
