module org.personnal.serveur {
    requires javafx.controls;
    requires javafx.fxml;
    requires jbcrypt;
    requires com.google.gson;
    requires java.sql;


    opens org.personnal.serveur to javafx.fxml;
    exports org.personnal.serveur;
    opens org.personnal.serveur.protocol to com.google.gson;
}