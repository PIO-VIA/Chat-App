package org.personnal.client.model.message;

public class FailedMessageState implements IMessageStatusState{
    @Override
    public boolean canBeEdited() {
        return true;   // On peut éditer pour réessayer
    }

    @Override
    public boolean canBeDeleted() {
        return true;
    }

    @Override
    public String getStatusLabel() {
        return Message.STATUS_FAILED;
    }
}
