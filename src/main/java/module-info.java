module org.personnal.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.desktop;
    requires java.sql;


    opens org.personnal.client to javafx.fxml;
    exports org.personnal.client;

    opens org.personnal.client.network to com.google.gson;
    exports org.personnal.client.network to com.google.gson;


}