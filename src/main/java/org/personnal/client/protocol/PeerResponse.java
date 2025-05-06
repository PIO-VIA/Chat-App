package org.personnal.client.protocol;

import java.io.Serializable;

public class PeerResponse implements Serializable {
    private RequestType type;
    private boolean success;
    private String message;
    private Object data; // Peut contenir un User, un Message, etc.

    public PeerResponse(boolean success, String message, Object o){}
    public PeerResponse(boolean success, String message) {
        this(success, message, null);
    }

    public PeerResponse(RequestType type,boolean success, String message, Object data) {
        this.type = type;
        this.success = success;
        this.message = message;
        this.data = data;
    }
    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
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

    public void setData(Object data) {this.data = data;}

    public void setMessage(String message) {this.message = message;}

    public void setSuccess(boolean success) {this.success = success;}
}

