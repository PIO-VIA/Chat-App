package org.personnal.serveur.protocol;


public enum RequestType {
    REGISTER,
    LOGIN,
    CALL,
    DISCONNECT,
    SEND_MESSAGE,
    SEND_FILE,
    CHECK_USER,
    CHECK_ONLINE,
    PING,        // Nouveau type pour les requêtes de ping servant à maintenir la connexion active
    TYPING

}
