package org.personnal.serveur.protocol;

import java.io.Serializable;

public class PeerResponse implements Serializable {
    private boolean success;
    private String message;
    private Object data; // Peut contenir un User, un Message, etc.

    public PeerResponse(boolean success, String message) {
        this(success, message, null);
    }

    public PeerResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
