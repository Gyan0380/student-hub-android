package com.studenthub.app.model;

/**
 * Plain data model representing a single chat message.
 *
 * IMPORTANT: This class maps directly to the JSON structure stored under
 * the "messages" node in Firebase Realtime Database:
 *
 * messages/
 *   -Nabc123.../
 *     senderId: "device-generated-or-auth-uid"
 *     senderName: "Gyan"
 *     text: "Hello from Android!"
 *     timestamp: 1699999999999   (server time in millis)
 *
 * The Web App must write to the SAME "messages" node with the SAME field
 * names for cross-platform sync to work. If the Web App uses different
 * field names, update the getters/setters below to match.
 *
 * A no-argument constructor is REQUIRED for Firebase to be able to
 * deserialize snapshots into this class automatically via
 * dataSnapshot.getValue(ChatMessage.class).
 */
public class ChatMessage {

    private String senderId;
    private String senderName;
    private String text;
    private Long timestamp;

    // Required empty constructor for Firebase deserialization
    public ChatMessage() {
    }

    public ChatMessage(String senderId, String senderName, String text, Long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
