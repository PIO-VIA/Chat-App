package org.personnal.client.model;

import java.io.Serializable;

public class Message implements Serializable {
    private String sender;
    private String receiver;
    private String content;
    private long timestamp;
    private boolean read;

    // Constructeurs
    public Message() {}

    public Message(String sender, String receiver, String content, long timestamp, boolean read) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
        this.read = read;
    }

    // Getters
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }

    // Setters
    public void setSender(String sender) { this.sender = sender; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { this.read = read; }

    @Override
    public String toString() {
        return "Message{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", read=" + read +
                '}';
    }
}