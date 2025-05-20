module org.personnal.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.sql;
    requires java.desktop;
    requires java.prefs;

    opens org.personnal.client to javafx.fxml;
    exports org.personnal.client;
    opens org.personnal.client.protocol to com.google.gson;
    opens org.personnal.client.model to com.google.gson;
}