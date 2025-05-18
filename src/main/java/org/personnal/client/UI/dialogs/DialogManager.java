package org.personnal.client.UI.dialogs;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Pair;
import org.personnal.client.UI.ChatView;
import org.personnal.client.controller.ChatController;
import org.personnal.client.model.User;

import java.util.Optional;

/**
 * Gestionnaire des boîtes de dialogue
 */
public class DialogManager {
    private final ChatController controller;
    private final ChatView chatView;

    /**
     * Constructeur du gestionnaire de dialogues
     * @param controller Contrôleur de chat
     * @param chatView Vue de chat
     */
    public DialogManager(ChatController controller, ChatView chatView) {
        this.controller = controller;
        this.chatView = chatView;
    }

    /**
     * Affiche le dialogue d'ajout de contact
     */
    public void showAddContactDialog() {
        // Créer un dialogue personnalisé
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un contact");
        dialog.setHeaderText("Entrez les informations du contact à ajouter");

        // Définir les boutons
        ButtonType addButtonType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Créer les champs du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        grid.add(new Label("Nom d'utilisateur:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Donner le focus au champ username
        Platform.runLater(() -> usernameField.requestFocus());

        // Convertir le résultat du dialogue
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new Pair<>(usernameField.getText(), emailField.getText());
            }
            return null;
        });

        // Afficher le dialogue et traiter le résultat
        dialog.showAndWait().ifPresent(usernameEmail -> {
            String username = usernameEmail.getKey().trim();
            String email = usernameEmail.getValue().trim();

            if (!username.isEmpty()) {
                boolean added = controller.addContact(username, email);
                if (added) {
                    // Rafraîchir la liste des contacts depuis la BD locale
                    chatView.updateContactList();

                    // Afficher un message de confirmation
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Contact ajouté");
                    alert.setHeaderText(null);
                    alert.setContentText("Le contact " + username + " a été ajouté avec succès à votre liste de contacts.");
                    alert.showAndWait();
                } else {
                    // Afficher un message d'erreur
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText(null);
                    alert.setContentText("Impossible d'ajouter le contact " + username + ".\nVérifiez que l'utilisateur existe et qu'il n'est pas déjà dans vos contacts.");
                    alert.showAndWait();
                }
            }
        });
    }

    /**
     * Affiche le dialogue des paramètres
     */
    public void showSettingsDialog() {
        // Créer un dialogue pour les paramètres
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Paramètres");
        dialog.setHeaderText("Paramètres utilisateur");

        // Ajouter des onglets pour différentes sections
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Onglet Profil
        Tab profileTab = new Tab("Profil");
        GridPane profileGrid = new GridPane();
        profileGrid.setHgap(10);
        profileGrid.setVgap(10);
        profileGrid.setPadding(new Insets(20, 150, 10, 10));

        // Informations sur l'utilisateur actuel
        Label usernameLabel = new Label("Nom d'utilisateur: " + controller.getCurrentUsername());
        Label statusLabel = new Label("Statut: En ligne");

        profileGrid.add(usernameLabel, 0, 0);
        profileGrid.add(statusLabel, 0, 1);

        // Bouton de déconnexion
        Button logoutButton = new Button("Déconnexion");
        logoutButton.setOnAction(e -> {
            controller.disconnect();
            dialog.close();
            // Rediriger vers l'écran de connexion (à implémenter)
        });

        profileGrid.add(logoutButton, 0, 3);
        profileTab.setContent(profileGrid);

        // Onglet Contacts
        Tab contactsTab = new Tab("Contacts");
        VBox contactsBox = new VBox(10);
        contactsBox.setPadding(new Insets(10));

        // Liste des contacts pour gestion (suppression, blocage, etc.)
        ListView<User> contactsListView = new ListView<>();
        contactsListView.setItems(controller.getUsersList());
        contactsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    // Avatar
                    Circle avatar = new Circle(15, Color.LIGHTBLUE);
                    Text initial = new Text(user.getUsername().substring(0, 1).toUpperCase());
                    initial.setFill(Color.WHITE);
                    StackPane avatarPane = new StackPane(avatar, initial);

                    // Infos utilisateur
                    VBox userInfo = new VBox(2);
                    Label username = new Label(user.getUsername());
                    username.setFont(Font.font("Arial", FontWeight.BOLD, 13));
                    Label email = new Label(user.getEmail() != null ? user.getEmail() : "");
                    email.setFont(Font.font("Arial", 11));
                    email.setTextFill(Color.GRAY);

                    userInfo.getChildren().addAll(username, email);
                    box.getChildren().addAll(avatarPane, userInfo);

                    setGraphic(box);
                }
            }
        });

        Button deleteContactButton = new Button("Supprimer le contact sélectionné");
        deleteContactButton.setDisable(true);

        contactsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newValue) ->
                deleteContactButton.setDisable(newValue == null));

        deleteContactButton.setOnAction(e -> {
            User selectedUser = contactsListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirmation");
                confirmAlert.setHeaderText("Supprimer un contact");
                confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer " + selectedUser.getUsername() + " de vos contacts?");

                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // Supprimer le contact de la BD locale
                    if (controller.deleteContact(selectedUser.getIdUser())) {
                        // Mettre à jour la liste des contacts
                        contactsListView.setItems(controller.getUsersList());
                        chatView.updateContactList();

                        // Afficher un message de confirmation
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Contact supprimé");
                        successAlert.setHeaderText(null);
                        successAlert.setContentText("Le contact a été supprimé avec succès.");
                        successAlert.showAndWait();
                    }
                }
            }
        });

        contactsBox.getChildren().addAll(
                new Label("Gérer vos contacts:"),
                contactsListView,
                deleteContactButton
        );

        contactsTab.setContent(contactsBox);

        // Ajouter les onglets au TabPane
        tabPane.getTabs().addAll(profileTab, contactsTab);

        // Ajouter les boutons au dialogue
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(tabPane);

        // Afficher le dialogue
        dialog.showAndWait();
    }
}