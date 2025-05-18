package org.personnal.client.database.DAO;


import org.personnal.client.model.Message;

import java.util.List;

public interface IMessageDAO {
    void saveMessage(Message message);
    List<Message> getMessagesWith(String username);
    void deleteMessageById(int id);
    void markMessagesAsRead(String sender, String receiver);
}

