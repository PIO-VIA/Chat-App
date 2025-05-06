/**
 * Interface pour le statut d'un message
 * Pattern State pour gérer l'état du message
 */

package org.personnal.client.model.message;

public interface IMessageStatusState {
    boolean canBeEdited();
    boolean canBeDeleted();
    String getStatusLabel();
}
