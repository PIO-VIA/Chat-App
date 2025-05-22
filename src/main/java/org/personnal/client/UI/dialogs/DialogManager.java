package org.personnal.client.UI.dialogs;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.personnal.client.controller.ChatController;
import org.personnal.client.model.User;
import org.personnal.client.UI.call.AudioPreferencesWindow;

import java.util.Optional;

import static java.awt.SystemColor.menu;

/**
 * Classe pour gérer tous les dialogues de l'application avec styles améliorés
 */
public class DialogManager {
    private final ChatController controller;
    private final Stage mainStage;

    // Constantes pour les icônes (à remplacer par les chemins réels vers vos icônes)
    private static final String ICON_USER = "\uf007";       // FontAwesome user icon
    private static final String ICON_SETTINGS = "\uf013";   // FontAwesome settings icon
    private static final String ICON_FILES = "\uf15b";      // FontAwesome file icon
    private static final String ICON_EMAIL = "\uf0e0";      // FontAwesome email icon
    private static final String ICON_KEY = "\uf084";        // FontAwesome key icon
    private static final String ICON_LOGOUT = "\uf08b";     // FontAwesome sign-out icon
    private static final String ICON_CONTACTS = "\uf0c0";   // FontAwesome users icon
    private static final String ICON_ADD = "\uf067";        // FontAwesome plus icon
    private static final String ICON_TRASH = "\uf1f8";      // FontAwesome trash icon

    public DialogManager(ChatController controller, Stage mainStage) {
        this.controller = controller;
        this.mainStage = mainStage;
    }

    /**
     * Affiche le dialogue d'ajout de contact avec style amélioré
     */
    public void showAddContactDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un nouveau contact");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("add-contact-dialog");

        // Charger les feuilles de style
        dialogPane.getStylesheets().addAll(
                getClass().getResource("/styles/global.css").toExternalForm(),
                getClass().getResource("/styles/dialogs.css").toExternalForm()
        );

        // Conteneur principal
        VBox rootBox = new VBox(15);
        rootBox.setAlignment(Pos.CENTER);
        rootBox.setPadding(new Insets(0, 0, 10, 0));

        // Titre
        Label titleLabel = new Label("Ajouter un contact");
        titleLabel.setStyle(
                "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #333333; " +
                        "-fx-padding: 0 0 10px 0;"
        );

        // Champ nom d'utilisateur
        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");
        usernameField.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #dddddd; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-padding: 10px; " +
                        "-fx-pref-height: 40px;"
        );
        usernameField.setMaxWidth(Double.MAX_VALUE);

        // Champ email
        TextField emailField = new TextField();
        emailField.setPromptText("Email (optionnel)");
        emailField.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #dddddd; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-padding: 10px; " +
                        "-fx-pref-height: 40px;"
        );
        emailField.setMaxWidth(Double.MAX_VALUE);

        // Boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button cancelButton = new Button("Annuler");
        cancelButton.getStyleClass().add("cancel-button");
        cancelButton.setCancelButton(true);
        cancelButton.setMinWidth(100);

        Button addButton = new Button("Ajouter");
        addButton.getStyleClass().add("add-contact-button");
        addButton.setDefaultButton(true);
        addButton.setMinWidth(100);

        buttonBox.getChildren().addAll(cancelButton, addButton);

        // Message d'aide
        Label infoLabel = new Label(
                "Entrez le nom d'utilisateur exact du contact que vous souhaitez ajouter. " +
                        "L'email est optionnel et peut vous aider à identifier votre contact."
        );
        infoLabel.setWrapText(true);
        infoLabel.setStyle(
                "-fx-text-fill: #666666; " +
                        "-fx-font-size: 12px; " +
                        "-fx-padding: 10px 0 0 0;"
        );
        infoLabel.setAlignment(Pos.CENTER);

        // Ajout des éléments au conteneur principal
        rootBox.getChildren().addAll(titleLabel, usernameField, emailField, buttonBox, infoLabel);

        // Fixer la taille de la boîte de dialogue comme la page de connexion
        dialogPane.setContent(rootBox);
        dialogPane.setPrefWidth(420);
        dialogPane.setMinWidth(370);
        dialogPane.setMaxWidth(420);
        rootBox.setPrefWidth(400);
        rootBox.setMinWidth(370);
        rootBox.setMaxWidth(420);

        // Gestion des actions
        addButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            if (!username.isEmpty()) {
                if (controller.addContact(username, email)) {
                    showAlert(Alert.AlertType.INFORMATION, "Contact ajouté",
                            "Le contact " + username + " a été ajouté avec succès.");
                    dialog.setResult(ButtonType.OK);
                    dialog.close();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible d'ajouter le contact. Vérifiez que l'utilisateur existe et n'est pas déjà dans vos contacts.");
                }
            }
        });
        cancelButton.setOnAction(e -> {
            dialog.setResult(ButtonType.CANCEL);
            dialog.close();
        });
        // Fermer aussi avec la croix (bouton de fermeture)
        dialog.setOnCloseRequest(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        // Affichage du dialogue
        Platform.runLater(dialog::showAndWait);
    }

    /**
     * Affiche le dialogue des paramètres avec style amélioré
     */
    public void showSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        // Ne pas définir le propriétaire pour éviter les erreurs
        dialog.setTitle("Paramètres");
        dialog.setHeaderText("Paramètres utilisateur");

        // Appliquer les classes CSS
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().addAll("dialog-pane", "settings-dialog");
        dialogPane.setPrefWidth(500);
        dialogPane.setPrefHeight(400);

        // Icône en haut à gauche
        Label iconLabel = new Label(ICON_SETTINGS);
        iconLabel.setFont(Font.font("FontAwesome", 24));
        iconLabel.setTextFill(Color.WHITE);
        dialogPane.setGraphic(iconLabel);

        // Onglets
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("settings-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Onglet Profil
        Tab profileTab = new Tab("Profil");
        profileTab.setGraphic(createTabIcon(ICON_USER));

        VBox profileContent = new VBox(20);
        profileContent.getStyleClass().addAll("settings-tab", "profile-section");

        HBox profileHeader = new HBox(15);
        profileHeader.getStyleClass().add("profile-header");

        // Avatar de l'utilisateur
        Circle avatar = new Circle(30);
        avatar.getStyleClass().add("profile-avatar");
        Text initial = new Text(controller.getCurrentUsername().substring(0, 1).toUpperCase());
        initial.setFill(Color.WHITE);
        StackPane avatarPane = new StackPane(avatar, initial);

        VBox userInfo = new VBox(5);
        Label usernameLabel = new Label(controller.getCurrentUsername());
        usernameLabel.getStyleClass().add("profile-username");

        userInfo.getChildren().addAll(usernameLabel);
        profileHeader.getChildren().addAll(avatarPane, userInfo);

        // Sections des paramètres
        VBox accountSection = new VBox(10);
        accountSection.getStyleClass().add("settings-section");

        Label accountHeader = new Label("Compte");
        accountHeader.getStyleClass().add("settings-section-header");

        Button logoutButton = new Button("Déconnexion");
        logoutButton.setGraphic(createButtonIcon(ICON_LOGOUT));
        logoutButton.getStyleClass().add("settings-button");
        logoutButton.setOnAction(e -> {
            controller.disconnect();
            dialog.close();
            // Redirection vers l'écran de connexion géré par le MainClient
        });

        accountSection.getChildren().addAll(accountHeader, logoutButton);

        profileContent.getChildren().addAll(profileHeader, accountSection);
        profileTab.setContent(profileContent);

        // Onglet Contacts
        Tab contactsTab = new Tab("Contacts");
        contactsTab.setGraphic(createTabIcon(ICON_CONTACTS));

        VBox contactsContent = new VBox(20);
        contactsContent.getStyleClass().addAll("settings-tab", "contact-manager-section");

        Label contactsHeader = new Label("Gestion des contacts");
        contactsHeader.getStyleClass().add("settings-tab-header");

        VBox contactsSection = new VBox(10);
        contactsSection.getStyleClass().add("settings-section");

        ListView<String> contactListView = new ListView<>(controller.getContacts());
        contactListView.getStyleClass().add("contact-list-view");

        HBox contactActions = new HBox(10);
        contactActions.setAlignment(Pos.CENTER_RIGHT);

        Button deleteContactButton = new Button("Supprimer");
        deleteContactButton.setGraphic(createButtonIcon(ICON_TRASH));
        deleteContactButton.getStyleClass().addAll("settings-button", "destructive-button");
        deleteContactButton.setDisable(true);

        contactListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newValue) ->
                deleteContactButton.setDisable(newValue == null));

        deleteContactButton.setOnAction(e -> {
            String selectedContact = contactListView.getSelectionModel().getSelectedItem();
            if (selectedContact != null) {
                showConfirmDeleteContact(selectedContact, contactListView);
            }
        });

        contactActions.getChildren().add(deleteContactButton);
        contactsSection.getChildren().addAll(contactListView, contactActions);

        contactsContent.getChildren().addAll(contactsHeader, contactsSection);
        contactsTab.setContent(contactsContent);

        // Ajout des onglets
        tabPane.getTabs().addAll(profileTab, contactsTab);

        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Style du bouton Fermer
        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.getStyleClass().addAll("settings-button", "cancel-button");

        // Afficher le dialogue en utilisant Platform.runLater
        Platform.runLater(() -> dialog.showAndWait());
    }

    /**
     * Affiche une boîte de dialogue de confirmation pour supprimer un contact
     */
    private void showConfirmDeleteContact(String contactName, ListView<String> contactListView) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le contact");
        alert.setContentText("Voulez-vous vraiment supprimer " + contactName + " de vos contacts ?");

        // Style de l'alerte
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");

        // Icône en haut à gauche
        Label iconLabel = new Label(ICON_TRASH);
        iconLabel.setFont(Font.font("FontAwesome", 24));
        iconLabel.setTextFill(Color.WHITE);
        dialogPane.setGraphic(iconLabel);

        // Style des boutons
        ((Button) dialogPane.lookupButton(ButtonType.OK)).getStyleClass().addAll("settings-button", "destructive-button");
        ((Button) dialogPane.lookupButton(ButtonType.OK)).setText("Supprimer");
        ((Button) dialogPane.lookupButton(ButtonType.CANCEL)).getStyleClass().addAll("settings-button", "cancel-button");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Récupérer l'ID du contact
            User user = controller.getUserByUsername(contactName);
            if (user != null) {
                if (controller.deleteContact(user.getIdUser())) {
                    // Rafraîchir la liste
                    contactListView.setItems(controller.getContacts());

                    showAlert(Alert.AlertType.INFORMATION, "Contact supprimé",
                            "Le contact a été supprimé avec succès.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Une erreur est survenue lors de la suppression du contact.");
                }
            }
        }
    }

    public void showAudioPreferencesDialog() {
        AudioPreferencesWindow preferencesWindow = new AudioPreferencesWindow();
        preferencesWindow.show();
    }


    /**
     * Affiche une alerte générique
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        // Ne pas définir de propriétaire pour éviter les erreurs
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style de l'alerte
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");

        // Choisir l'icône en fonction du type d'alerte
        Label iconLabel = new Label();
        iconLabel.setFont(Font.font("FontAwesome", 24));
        iconLabel.setTextFill(Color.WHITE);

        if (type == Alert.AlertType.INFORMATION) {
            iconLabel.setText("\uf05a"); // info icon
        } else if (type == Alert.AlertType.WARNING) {
            iconLabel.setText("\uf071"); // warning icon
        } else if (type == Alert.AlertType.ERROR) {
            iconLabel.setText("\uf057"); // error icon
        }

        dialogPane.setGraphic(iconLabel);

        Platform.runLater(() -> alert.showAndWait());
    }

    /**
     * Crée une icône pour un onglet
     */
    private Label createTabIcon(String iconCode) {
        Label icon = new Label(iconCode);
        icon.setFont(Font.font("FontAwesome", 14));
        return icon;
    }

    /**
     * Crée une icône pour un bouton
     */
    private Label createButtonIcon(String iconCode) {
        Label icon = new Label(iconCode);
        icon.setFont(Font.font("FontAwesome", 14));
        icon.setPadding(new Insets(0, 5, 0, 0));
        return icon;
    }
}