package org.personnal.client.model.message;

abstract class MessageFactory {
    public abstract Message createTextMessage(String senderId, String recipientId, String conversationId, String content);
    public abstract Message createImageMessage(String senderId, String recipientId, String conversationId, String imageUrl);
    public abstract Message createFileMessage(String senderId, String recipientId, String conversationId, String fileUrl, String fileName);
}
