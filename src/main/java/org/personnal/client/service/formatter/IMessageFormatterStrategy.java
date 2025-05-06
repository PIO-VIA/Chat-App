/**
 * Interface pour les stratégies de formatage de message
 * Pattern Strategy pour gérer différents types de formatages
 */

package org.personnal.client.service.formatter;

public interface IMessageFormatterStrategy {
    String format(String content);
    String getType();

}
