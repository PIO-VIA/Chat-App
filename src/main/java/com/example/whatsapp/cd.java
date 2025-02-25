package com.example.whatsapp;

import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

class ContactListCell extends ListCell<Contact> {
    @Override
    protected void updateItem(Contact contact, boolean empty) {
        super.updateItem(contact, empty);

        if (empty || contact == null) {
            setGraphic(null);
        } else {
            // Avatar (cercle coloré)
            Circle avatar = new Circle(20, Color.LIGHTGRAY);

            // Nom et statut
            Text name = new Text(contact.getName());
            Text status = new Text(contact.getStatus());
            status.setFill(Color.GRAY);

            // Conteneur pour le nom et le statut
            VBox textContainer = new VBox(5, name, status);

            // Conteneur principal pour l'avatar et les textes
            HBox contactItem = new HBox(10, avatar, textContainer);
            contactItem.setStyle("-fx-padding: 10;");

            setGraphic(contactItem);
        }
    }
}
