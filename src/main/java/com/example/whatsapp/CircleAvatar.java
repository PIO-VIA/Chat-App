package com.example.whatsapp;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

// Classe utilitaire pour les avatars circulaires (simplifié)
class CircleAvatar extends StackPane {
    public CircleAvatar() {
        Circle circle = new Circle(25);
        circle.setFill(Color.LIGHTGRAY);
        this.getChildren().add(circle);
    }
}