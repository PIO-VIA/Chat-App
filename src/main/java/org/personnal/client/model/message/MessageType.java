/**
 * Enum pour les types de message
 * Utile pour le pattern Factory et pour appliquer le principe Open/Closed
 */

package org.personnal.client.model.message;

enum MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    FILE,
    AUDIO,
    LOCATION,
    STICKER,
    SYSTEM_NOTIFICATION
}
