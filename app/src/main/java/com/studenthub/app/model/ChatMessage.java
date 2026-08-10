package com.studenthub.app.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Maps directly to a document under Chats/{roomId}/Messages in Firestore —
 * the SAME collection path and field names the Web App (app.js) uses. That
 * shared schema is what makes chat cross-platform: a message written here
 * shows up on the website instantly, and vice-versa.
 */
public class ChatMessage {

    private String senderId;
    private String senderName;
    private String senderPhoto;
    private String text;

    @ServerTimestamp
    private Date createdAt;

    private boolean edited;

    @Exclude
    private String id; // Firestore document id, filled in after reading

    public ChatMessage() {
        // Required empty constructor for Firestore deserialization
    }

    public ChatMessage(String senderId, String senderName, String senderPhoto, String text) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderPhoto = senderPhoto;
        this.text = text;
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhoto() { return senderPhoto; }
    public void setSenderPhoto(String senderPhoto) { this.senderPhoto = senderPhoto; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @PropertyName("edited")
    public boolean isEdited() { return edited; }
    @PropertyName("edited")
    public void setEdited(boolean edited) { this.edited = edited; }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }
}
