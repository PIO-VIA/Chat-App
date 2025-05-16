package org.personnal.serveur.protocol;


public enum RequestType {
    REGISTER,
    LOGIN,
    CALL,
    DISCONNECT,
    SEND_MESSAGE,
    SEND_FILE,
    CHECK_USER,       // Vérifier si un utilisateur existe
    CHECK_ONLINE,

}
