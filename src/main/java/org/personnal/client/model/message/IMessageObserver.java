/**
 * Interface MessageObserver
 * Pattern Observer pour notifier des événements liés aux messages
 */

package org.personnal.client.model.message;

public interface IMessageObserver {
    void onMessageSent(Message message);
    void onMessageDelivered(Message message);
    void onMessageRead(Message message);
    void onMessageEdited(Message message, String oldContent);

}
