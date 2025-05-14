package org.personnal.client.controller;

import org.personnal.client.MainClient;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import java.io.IOException;
import java.util.Map;

public class LoginController {
    private final MainClient app;
    private final ClientSocketManager socketManager;

    public LoginController(MainClient app, ClientSocketManager socketManager) {
        this.app = app;
        this.socketManager = socketManager;
    }

    public void handleLogin(String username, String password) throws IOException {
        validateCredentials(username, password);

        PeerRequest request = createLoginRequest(username, password);
        socketManager.sendRequest(request);

        PeerResponse response = socketManager.readResponse();
        validateResponse(response);

        app.showChatView(username);
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom d'utilisateur est requis.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est requis.");
        }
    }

    private PeerRequest createLoginRequest(String username, String password) {
        return new PeerRequest(RequestType.LOGIN, Map.of(
                "username", username,
                "password", password
        ));
    }

    private void validateResponse(PeerResponse response) {
        if (!response.isSuccess()) {
            throw new IllegalStateException(
                    response.getMessage() != null ?
                            response.getMessage() : "Échec de la connexion pour une raison inconnue."
            );
        }
    }
}