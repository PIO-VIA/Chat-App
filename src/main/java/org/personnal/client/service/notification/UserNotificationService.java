package org.personnal.client.service.notification;

import org.personnal.client.model.IUserObserver;
import org.personnal.client.model.User;

public class UserNotificationService implements IUserObserver {
    private final INotificationService notificationService;

    public UserNotificationService(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onUserStatusChanged(User user, String oldStatus, String newStatus) {
        if (!oldStatus.equals(newStatus)) {
            // Notifier les contacts de l'utilisateur du changement de statut
            notificationService.notify(
                    String.format("L'utilisateur %s est passé du statut %s à %s",
                            user.getUsername(),
                            oldStatus,
                            newStatus
                    )
            );
        }
    }

    @Override
    public void onUserProfileUpdated(User user) {
        // Notifier que le profil a été mis à jour
        notificationService.notify(
                String.format("Le profil de l'utilisateur %s a été mis à jour", user.getUsername())
        );
    }
}
