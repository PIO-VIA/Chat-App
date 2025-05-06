package org.personnal.client.model.user;

public enum UserStatus implements IUserStatus {
    ONLINE {
        @Override public boolean canSendMessage()   { return true; }
        @Override public boolean canReceiveMessage(){ return true; }
        @Override public String  getStatusLabel()   { return name(); }//retourne le nom de la constante ONLINE
    },
    OFFLINE {
        @Override public boolean canSendMessage()   { return false; }
        @Override public boolean canReceiveMessage(){ return false; }
        @Override public String  getStatusLabel()   { return name(); }
    },
    BUSY {
        @Override public boolean canSendMessage()   { return true; }
        @Override public boolean canReceiveMessage(){ return false; }
        @Override public String  getStatusLabel()   { return name(); }
    };
}
