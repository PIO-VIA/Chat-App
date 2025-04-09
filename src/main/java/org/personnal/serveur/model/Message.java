package org.personnal.serveur.model;

import java.io.Serializable;

public class Message implements Serializable {
    private String sender;  // Expéditeur
    private String receiver; // Destinataire
    private String content; // Contenu du message
    private long timestamp;// Heure
    private boolean read;

    public Message(String sender, String receiver, String content, long timestamp,boolean read) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
        this.read=read;
    }
    public Message(){}
    public String getSender() {return sender;}

    public String getReceiver() {return receiver;}

    public String getContent() {return content;}

    public long getTimestamp() {return timestamp;}

    public boolean isRead() {return read;}

    public void setContent(String content) {this.content = content;}

    public void setRead(boolean read) {this.read = read;}

    public void setReceiver(String receiver) {this.receiver = receiver;}

    public void setSender(String sender) {this.sender = sender;}

    public void setTimestamp(long timestamp) {this.timestamp = timestamp;}

    @Override
    public String toString() {
        return "Message{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp + '\'' +
                ", read=" + read +
                '}';
    }
}
