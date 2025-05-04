/**
 * Interface UserStateInterface
 * Pattern State pour gérer les différents états d'un utilisateur
 */

package org.personnal.client.model;

public interface IUserStatus {
    boolean canSendMessage();
    boolean canReceiveMessage();
    String getStatusLabel();
}
