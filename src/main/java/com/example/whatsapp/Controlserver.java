package com.example.whatsapp;

import javafx.scene.control.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;


public class Controlserver  extends VBox{
    protected  double spacing;
    public Controlserver(double spacing){
        super (spacing);
        Menu file= new Menu("fichier");
        MenuItem Item =new MenuItem("noew");
        MenuItem ite =new MenuItem("new");
        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem exitItem= new MenuItem("quiter");
        CheckMenuItem tet= new CheckMenuItem("cjhoix");
        RadioMenuItem radio= new RadioMenuItem("salut");
        file.getItems().addAll(Item,ite,sep,exitItem,tet,radio);
        MenuBar menu =new MenuBar(file);
        Label label =new Label("clic bojjour ici  et joyeux noel");
        ContextMenu conte= new ContextMenu();
        conte.getItems().addAll(ite, Item);
        label.setContextMenu(conte);
        this.getChildren().addAll(menu, label);

    }
}
