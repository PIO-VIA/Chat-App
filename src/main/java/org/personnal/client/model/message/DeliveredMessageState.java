package org.personnal.client.model.message;

public class DeliveredMessageState implements IMessageStatusState{
    @Override
    public boolean canBeEdited() {
        return true;
    }

    @Override
    public boolean canBeDeleted() {
        return true;
    }

    @Override
    public String getStatusLabel() {
        return Message.STATUS_DELIVERED;
    }

}
