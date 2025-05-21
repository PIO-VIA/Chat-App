module org.personnal.serveur {
    requires javafx.controls;
    requires javafx.fxml;
    requires jbcrypt;
    requires com.google.gson;
    requires java.sql;


    opens org.personnal.serveur to javafx.fxml;
    exports org.personnal.serveur;
    exports org.personnal.serveur.model;
    exports org.personnal.serveur.protocol;
    exports org.personnal.serveur.network;

    opens org.personnal.serveur.protocol to com.google.gson;
    opens org.personnal.serveur.model to com.google.gson;
    exports org.personnal.serveur.test;
    opens org.personnal.serveur.test to javafx.fxml;
}