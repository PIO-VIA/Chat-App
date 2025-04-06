package org.personnal.serveur.network;

import org.personnal.serveur.model.User;

public interface IPeerService {
    void registerPeer(User user, String ip, int port);
    void unregisterPeer(String username);
    PeerInfo getPeerInfo(String username);
}