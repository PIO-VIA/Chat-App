package org.personnal.client.controller;

import org.personnal.client.MainClient;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;

import java.io.IOException;
import java.util.Map;

public class RegisterController {
    private final MainClient app;
    private final ClientSocketManager socketManager;

    public RegisterController(MainClient app, ClientSocketManager socketManager) {
        this.app = app;
        this.socketManager = socketManager;
    }

    public void handleRegistration(String username, String email, String password, String confirmPassword) throws IOException {
        validateRegistrationData(username, email, password, confirmPassword);

        PeerRequest request = createRegistrationRequest(username, email, password);
        socketManager.sendRequest(request);

        PeerResponse response = socketManager.readResponse();
        validateRegistrationResponse(response);

        app.showLoginView();
    }

    private void validateRegistrationData(String username, String email, String password, String confirmPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom d'utilisateur est requis.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est requis.");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Veuillez entrer une adresse email valide.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est requis.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas.");
        }
    }

    private boolean isValidEmail(String email) {
        // Validation simple d'email - pourrait être améliorée
        return email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    private PeerRequest createRegistrationRequest(String username, String email, String password) {
        return new PeerRequest(RequestType.REGISTER, Map.of(
                "username", username,
                "email", email,
                "password", password
        ));
    }

    private void validateRegistrationResponse(PeerResponse response) {
        if (!response.isSuccess()) {
            throw new IllegalStateException(
                    response.getMessage() != null ?
                            response.getMessage() : "Échec de l'inscription pour une raison inconnue."
            );
        }
    }
}