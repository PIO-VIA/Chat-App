package org.personnal.client.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {

    private int idMessage;
    private String sender;
    private String receiver;
    private String content;
    private LocalDateTime timestamp;
    private boolean read;

    public Message(int idMessage, String sender, String receiver, String content, LocalDateTime timestamp, boolean read) {
        this.idMessage = idMessage;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
        this.read = read;
    }

    public Message() {}
    public int getIdMessage() { return idMessage; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isRead() {return read;}
    public void setIdMessage(int id) { this.idMessage = id; }
    public void setSender(String sender) { this.sender = sender; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public void setRead(boolean read) {this.read = read;}

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