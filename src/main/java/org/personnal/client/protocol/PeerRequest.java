package org.personnal.client.protocol;

import java.io.Serializable;
import java.util.Map;

public class PeerRequest implements Serializable {

    private RequestType type;
    private Map<String, String> payload;
    public PeerRequest() {}
    public PeerRequest(RequestType type, Map<String, String> payload) {
        this.type = type;
        this.payload = payload;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, String> payload) {
        this.payload = payload;
    }
}
