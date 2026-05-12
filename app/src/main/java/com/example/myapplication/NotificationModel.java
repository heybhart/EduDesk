package com.example.myapplication;

public class NotificationModel {
    private String documentId;
    private String title;
    private String body;
    private String type;
    private long timestamp;
    private boolean read;
    private String userId;
    private String profileImageUrl;

    public NotificationModel() {
        // Required for Firestore
    }

    public NotificationModel(String title, String body, String type, long timestamp, boolean read, String userId) {
        this.title = title;
        this.body = body;
        this.type = type;
        this.timestamp = timestamp;
        this.read = read;
        this.userId = userId;
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}
