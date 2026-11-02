package com.example.mealsync.data.models;

public class UserProfile {
    private String uid;
    private String email;
    private String displayName;
    private String messId;
    private String role;
    private long createdAt;

    public UserProfile() {
    }

    public UserProfile(String uid, String email, String displayName, String messId, String role, long createdAt) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.messId = messId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMessId() {
        return messId;
    }

    public void setMessId(String messId) {
        this.messId = messId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
