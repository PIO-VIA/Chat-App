package org.personnal.client.UI.utils;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Utilitaires pour créer des éléments d'interface utilisateur réutilisables
 */
public class UIComponentFactory {

    /**
     * Crée un avatar circulaire avec les initiales d'un utilisateur
     *
     * @param username Nom d'utilisateur
     * @param size Taille du cercle
     * @param backgroundColor Couleur de fond
     * @param textColor Couleur du texte
     * @return StackPane contenant l'avatar
     */
    public static StackPane createUserAvatar(String username, double size, Color backgroundColor, Color textColor) {
        Circle avatar = new Circle(size, backgroundColor);
        String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
        Text initialText = new Text(initial);
        initialText.setFill(textColor);
        initialText.setFont(Font.font("Arial", FontWeight.BOLD, size * 0.8));

        return new StackPane(avatar, initialText);
    }

    /**
     * Crée un indicateur de statut en ligne
     *
     * @param isOnline Statut en ligne
     * @param size Taille du cercle
     * @return Circle représentant le statut
     */
    public static Circle createOnlineStatusIndicator(boolean isOnline, double size) {
        Circle statusCircle = new Circle(size);
        statusCircle.setFill(isOnline ? Color.GREEN : Color.GRAY);
        return statusCircle;
    }

    /**
     * Crée un badge de notification
     *
     * @param count Nombre de notifications (0 pour simplement un point)
     * @param size Taille du cercle
     * @return StackPane contenant le badge
     */
    public static StackPane createNotificationBadge(int count, double size) {
        Circle badge = new Circle(size, Color.RED);

        Text countText;
        if (count <= 0) {
            countText = new Text("");
        } else if (count > 99) {
            countText = new Text("99+");
        } else {
            countText = new Text(String.valueOf(count));
        }

        countText.setFill(Color.WHITE);
        countText.setFont(Font.font("Arial", FontWeight.BOLD, size * 0.8));

        return new StackPane(badge, countText);
    }

    /**
     * Crée un indicateur simple (point rouge) pour les messages non lus
     *
     * @param size Taille du cercle
     * @return StackPane contenant l'indicateur
     */
    public static StackPane createUnreadIndicator(double size) {
        Circle unreadIndicator = new Circle(size, Color.RED);
        Text unreadText = new Text("!");
        unreadText.setFill(Color.WHITE);
        unreadText.setFont(Font.font("Arial", FontWeight.BOLD, size * 0.8));

        return new StackPane(unreadIndicator, unreadText);
    }
}