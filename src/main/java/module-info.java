module org.personnal.client {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.personnal.client to javafx.fxml;
    exports org.personnal.client;
}