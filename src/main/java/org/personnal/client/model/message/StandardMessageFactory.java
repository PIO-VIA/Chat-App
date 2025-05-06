package org.personnal.client.model.message;

import org.personnal.client.service.formatter.EmojiFormatter;
import org.personnal.client.service.formatter.UrlFormatter;
import org.personnal.client.service.validation.LengthValidator;

import java.util.UUID;

public class StandardMessageFactory extends MessageFactory{
    @Override
    public Message createTextMessage(String senderId, String recipientId, String conversationId, String content) {
        return Message.builder()
                .withId(UUID.randomUUID().toString())
                .withSenderId(senderId)
                .withRecipientId(recipientId)
                .withConversationId(conversationId)
                .withContent(content)
                .withType(MessageType.TEXT)
                .withValidator(new LengthValidator(1, 5000))
                .withFormatter(new EmojiFormatter())
                .withFormatter(new UrlFormatter())
                .build();
    }

    @Override
    public Message createImageMessage(String senderId, String recipientId, String conversationId, String imageUrl) {
        Message message = Message.builder()
                .withId(UUID.randomUUID().toString())
                .withSenderId(senderId)
                .withRecipientId(recipientId)
                .withConversationId(conversationId)
                .withContent("Image partagée")
                .withType(MessageType.IMAGE)
                .build();

        message.addMetadata("imageUrl", imageUrl);
        return message;
    }

    @Override
    public Message createFileMessage(String senderId, String recipientId, String conversationId, String fileUrl, String fileName) {
        Message message = Message.builder()
                .withId(UUID.randomUUID().toString())
                .withSenderId(senderId)
                .withRecipientId(recipientId)
                .withConversationId(conversationId)
                .withContent("Fichier partagé: " + fileName)
                .withType(MessageType.FILE)
                .build();

        message.addMetadata("fileUrl", fileUrl);
        message.addMetadata("fileName", fileName);
        return message;
    }
}
