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
        setting.getStyleClass().add("gear");
        Button moi =new Button("moi");
        Menu.getChildren().addAll(setting, moi);
        barre.setLeft(Menu);
        barre.getStyleClass().add("sidebar");
        // deuxieme partie

        BorderPane Lcontact= new BorderPane();
        VBox LC=new VBox();
        HBox search =new HBox();
        Button recherche =new Button("search");
        TextField text= new TextField("search");
        text.getStyleClass().add("search-field");
        search.getChildren().addAll(recherche,text);
        LC.getChildren().addAll(search);
        Lcontact.setLeft(LC);
        Lcontact.getStyleClass().add("search-box");
        //troisieme partie
        BorderPane part =new BorderPane();
        HBox details= new HBox();
        VBox talk = new VBox();
        HBox tools =new HBox();
        Label label =new Label("name");
        label.getStyleClass().add("name");
        Button audio =new Button("call");
        Button video = new Button("video");
        details.getChildren().addAll(label,audio,video);
        // gerer l'affichage des messages
        talk.getStyleClass().add("chat-container");
        details.getStyleClass().add("chat-header");
        tools.getStyleClass().add("tools");


        Button emoji= new Button("emoji");
        Button file = new Button("fichier");
        TextField message =new TextField("write...");
        Button send = new Button("send");
        send.getStyleClass().add("send");
        message.getStyleClass().add("input");


        tools.getChildren().addAll(emoji, file, message, send);
        part.setBottom(tools);
        part.setTop(details);

        part.setCenter(talk);




        this.getChildren().addAll(barre,Lcontact,part);
    }
}
