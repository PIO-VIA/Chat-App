package org.personnal.client.model.message;

public class ReadMessageState implements IMessageStatusState{
    @Override
    public boolean canBeEdited() {
        return false;  // Une fois lu, on ne peut plus éditer
    }

    @Override
    public boolean canBeDeleted() {
        return true;   // Mais on peut toujours supprimer
    }

    @Override
    public String getStatusLabel() {
        return Message.STATUS_READ;
    }
}
