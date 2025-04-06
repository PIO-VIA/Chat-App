package org.personnal.serveur.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PeerRegistry {
    private static PeerRegistry instance;
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();

    private PeerRegistry() {}

    public static synchronized PeerRegistry getInstance() {
        if (instance == null) {
            instance = new PeerRegistry();
        }
        return instance;
    }

    public void register(String username, PeerInfo peerInfo) {
        peers.put(username, peerInfo);
    }

    public void unregister(String username) {
        peers.remove(username);
    }

    public PeerInfo getPeer(String username) {
        return peers.get(username);
    }
}