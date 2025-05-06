package org.personnal.client.service.notification;

import org.personnal.client.model.message.IMessageObserver;
import org.personnal.client.model.message.Message;

public class MessageNotificationService implements IMessageObserver {
    // Ce service pourrait notifier les utilisateurs via différents canaux

    @Override
    public void onMessageSent(Message message) {
        System.out.println("Message envoyé: " + message.getId());
    }

    @Override
    public void onMessageDelivered(Message message) {
        System.out.println("Message délivré: " + message.getId());
    }

    @Override
    public void onMessageRead(Message message) {
        System.out.println("Message lu: " + message.getId());
    }

    @Override
    public void onMessageEdited(Message message, String oldContent) {
        System.out.println("Message édité: " + message.getId());
        System.out.println("Ancien contenu: " + oldContent);
        System.out.println("Nouveau contenu: " + message.getContent());
    }
}
