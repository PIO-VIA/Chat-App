/**
 * Interface UserObserver
 * Mise en place du pattern Observer pour notifier des changements sur l'utilisateur
 */

package org.personnal.client.model.user;

public interface IUserObserver {
    void onUserStatusChanged(User user, String oldStatus, String newStatus);
    void onUserProfileUpdated(User user);
}
