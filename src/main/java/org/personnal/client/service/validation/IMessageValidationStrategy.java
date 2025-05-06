/**
 * Interface pour les stratégies de validation de message
 * Pattern Strategy pour gérer différentes validations
 */

package org.personnal.client.service.validation;

public interface IMessageValidationStrategy {
    boolean isValid(String content);
    String getErrorMessage();
}
