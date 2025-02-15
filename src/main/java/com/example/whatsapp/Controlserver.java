package com.example.whatsapp;

import javafx.scene.control.*;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class Controlserver  extends HBox {
    protected  double spacing;
    public Controlserver(double spacing){
        super (spacing);
        this.getStyleClass().add("tout");
        // premiere partie
        BorderPane barre = new BorderPane();
        VBox Menu= new VBox();
        Menu.setSpacing(10);
        Button setting =new Button("parametre");
        Button moi =new Button("moi");
        Menu.getChildren().addAll(setting, moi);
        barre.setLeft(Menu);
        // deuxieme partie

        BorderPane Lcontact= new BorderPane();
        VBox LC=new VBox();
        HBox search =new HBox();
        Button recherche =new Button("search");
        TextField text= new TextField("vous cherchez");
        search.getChildren().addAll(recherche,text);
        LC.getChildren().addAll(search);
        Lcontact.setLeft(LC);
        //troisieme partie
        BorderPane part =new BorderPane();
        HBox details= new HBox();
        VBox talk = new VBox();
        HBox tools =new HBox();
        Label label =new Label("name");
        Button audio =new Button("call");
        Button video = new Button("video");
        details.getChildren().addAll(label,audio,video);
        // gerer l'affichage des messages

        Button emoji= new Button("emoji");
        Button file = new Button("fichier");
        TextField message =new TextField();
        Button send = new Button("send");
        Button test =new Button("test");


        tools.getChildren().addAll(emoji, file, message, send);
        part.setBottom(tools);
        part.setTop(details);
        part.setRight(test);




        this.getChildren().addAll(barre,Lcontact,part);
    }
}
