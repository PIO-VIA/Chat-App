package org.personnal.client.model.message;

import org.personnal.client.service.formatter.IMessageFormatterStrategy;
import org.personnal.client.service.validation.IMessageValidationStrategy;

import java.time.LocalDateTime;
import java.util.*;

public class Message {
    // Constantes pour les statuts de message
    public static final String STATUS_SENT = "sent";
    public static final String STATUS_DELIVERED = "delivered";
    public static final String STATUS_READ = "read";
    public static final String STATUS_FAILED = "failed";

    private String id;
    private String senderId;
    private String recipientId;
    private String conversationId;
    private String content;
    private LocalDateTime timestamp;
    private LocalDateTime editedTimestamp;
    private MessageType type;
    private Map<String, Object> metadata;
    private IMessageStatusState status;
    private List<IMessageObserver> observers;
    private List<IMessageFormatterStrategy> formatters;
    private List<IMessageValidationStrategy> validators;

    /**
     * Constructeur privé pour forcer l'utilisation du builder
     */
    private Message() {
        this.timestamp = LocalDateTime.now();
        this.metadata = new HashMap<>();
        this.status = new SentMessageState();
        this.observers = new ArrayList<>();
        this.formatters = new ArrayList<>();
        this.validators = new ArrayList<>();
    }

    /**
     * Crée un MessageBuilder pour construire le message (Pattern Builder)
     */
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    /**
     * Pattern Factory Method pour créer des messages système
     */
    public static Message createSystemMessage(String conversationId, String content) {
        Message message = new Message();
        message.setId("system_" + UUID.randomUUID().toString());
        message.setSenderId("system");
        message.setRecipientId(null); // Destiné à tous les participants
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setType(MessageType.SYSTEM_NOTIFICATION);
        return message;
    }

    /**
     * Getters et setters avec validation
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        if (!validateContent(content)) {
            return; // Le contenu n'est pas valide
        }

        // Si c'est une modification, notifier les observateurs
        String oldContent = this.content;
        if (oldContent != null) {
            this.editedTimestamp = LocalDateTime.now();
            this.content = formatContent(content);
            notifyEdited(oldContent);
        } else {
            this.content = formatContent(content);
        }
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LocalDateTime getEditedTimestamp() {
        return editedTimestamp;
    }

    public boolean isEdited() {
        return editedTimestamp != null;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    /**
     * Gestion des metadata (données supplémentaires du message)
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }

    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(this.metadata);
    }

    /**
     * Gestion de l'état du message (Pattern State)
     */
    public IMessageStatusState getStatus() {
        return status;
    }

    public void setStatus(IMessageStatusState status) {
        this.status = status;
    }

    public void markAsDelivered() {
        if (STATUS_SENT.equals(this.status.getStatusLabel())) {
            this.status = new DeliveredMessageState();
            notifyDelivered();
        }
    }

    public void markAsRead() {
        if (STATUS_DELIVERED.equals(this.status.getStatusLabel())) {
            this.status = new ReadMessageState();
            notifyRead();
        }
    }

    public void markAsFailed() {
        this.status = new FailedMessageState();
    }

    public boolean canEdit() {
        return this.status.canBeEdited();
    }

    public boolean canDelete() {
        return this.status.canBeDeleted();
    }

    /**
     * Gestion des observateurs (Pattern Observer)
     */
    public void attachObserver(IMessageObserver observer) {
        this.observers.add(observer);
    }

    public void detachObserver(IMessageObserver observer) {
        this.observers.remove(observer);
    }

    private void notifySent() {
        for (IMessageObserver observer : observers) {
            observer.onMessageSent(this);
        }
    }

    private void notifyDelivered() {
        for (IMessageObserver observer : observers) {
            observer.onMessageDelivered(this);
        }
    }

    private void notifyRead() {
        for (IMessageObserver observer : observers) {
            observer.onMessageRead(this);
        }
    }

    private void notifyEdited(String oldContent) {
        for (IMessageObserver observer : observers) {
            observer.onMessageEdited(this, oldContent);
        }
    }

    /**
     * Gestion des stratégies de formatage (Pattern Strategy)
     */
    public void addFormatter(IMessageFormatterStrategy formatter) {
        this.formatters.add(formatter);
    }

    public void removeFormatter(IMessageFormatterStrategy formatter) {
        this.formatters.remove(formatter);
    }

    private String formatContent(String content) {
        String formattedContent = content;
        for (IMessageFormatterStrategy formatter : formatters) {
            formattedContent = formatter.format(formattedContent);
        }
        return formattedContent;
    }

    /**
     * Gestion des stratégies de validation (Pattern Strategy)
     */
    public void addValidator(IMessageValidationStrategy validator) {
        this.validators.add(validator);
    }

    public void removeValidator(IMessageValidationStrategy validator) {
        this.validators.remove(validator);
    }

    private boolean validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        for (IMessageValidationStrategy validator : validators) {
            if (!validator.isValid(content)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Méthode pour envoyer le message
     * Applique la logique métier pertinente
     */
    public void send() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }

        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }

        if (validateContent(this.content)) {
            notifySent();
        } else {
            markAsFailed();
        }
    }

    /**
     * Méthode pour transformer l'objet en Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("senderId", senderId);
        map.put("recipientId", recipientId);
        map.put("conversationId", conversationId);
        map.put("content", content);
        map.put("timestamp", timestamp.toString());
        map.put("type", type.name());
        map.put("status", status.getStatusLabel());
        map.put("isEdited", isEdited());
        if (isEdited()) {
            map.put("editedTimestamp", editedTimestamp.toString());
        }
        map.put("metadata", metadata);
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", from='" + senderId + '\'' +
                ", to='" + recipientId + '\'' +
                ", type=" + type +
                ", status=" + status.getStatusLabel() +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Builder pour la création d'objets Message (Pattern Builder)
     */
    public static class MessageBuilder {
        private final Message message;

        public MessageBuilder() {
            this.message = new Message();
        }

        public MessageBuilder withId(String id) {
            message.setId(id);
            return this;
        }

        public MessageBuilder withSenderId(String senderId) {
            message.setSenderId(senderId);
            return this;
        }

        public MessageBuilder withRecipientId(String recipientId) {
            message.setRecipientId(recipientId);
            return this;
        }

        public MessageBuilder withConversationId(String conversationId) {
            message.setConversationId(conversationId);
            return this;
        }

        public MessageBuilder withContent(String content) {
            message.setContent(content);
            return this;
        }

        public MessageBuilder withType(MessageType type) {
            message.setType(type);
            return this;
        }

        public MessageBuilder withMetadata(String key, Object value) {
            message.addMetadata(key, value);
            return this;
        }

        public MessageBuilder withFormatter(IMessageFormatterStrategy formatter) {
            message.addFormatter(formatter);
            return this;
        }

        public MessageBuilder withValidator(IMessageValidationStrategy validator) {
            message.addValidator(validator);
            return this;
        }

        public Message build() {
            return message;
        }
    }
}
