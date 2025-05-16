package org.personnal.client.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {

    private int id;
    private String sender;
    private String receiver;
    private String content;
    private LocalDateTime timestamp;
    private boolean isSentByMe;

    public Message(int id, String sender, String receiver, String content, LocalDateTime timestamp, boolean isSentByMe) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
    }

    public Message() {}
    public int getId() { return id; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSentByMe() { return isSentByMe; }
    public void setId(int id) { this.id = id; }
    public void setSender(String sender) { this.sender = sender; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setSentByMe(boolean sentByMe) { isSentByMe = sentByMe; }

    @Override
    public String toString() {
        return "Message{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", read=" + isSentByMe +
                '}';
    }
}