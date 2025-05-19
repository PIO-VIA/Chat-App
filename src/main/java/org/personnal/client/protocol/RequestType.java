package org.personnal.client.protocol;

public enum RequestType {
    REGISTER,
    LOGIN,
    CALL,
    CALL_VIDEO,
    DISCONNECT,
    SEND_MESSAGE,
    SEND_FILE,
    CHECK_USER,
    CHECK_ONLINE,
    PING,        // Nouveau type pour les requêtes de ping servant à maintenir la connexion active
    TYPING

}
