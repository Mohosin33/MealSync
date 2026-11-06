package com.example.mealsync.data.models;

public class Member {
    private String uid;
    private String name;
    private String email;
    private String role;
    private boolean includeInFixed = true;
    private long joinedAt;

    public Member() {
    }

    public Member(String uid, String name, String email, String role, boolean includeInFixed, long joinedAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.includeInFixed = includeInFixed;
        this.joinedAt = joinedAt;
    }

    public Member(String uid, String name, String email, String role, long joinedAt) {
        this(uid, name, email, role, true, joinedAt);
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isIncludeInFixed() {
        return includeInFixed;
    }

    public void setIncludeInFixed(boolean includeInFixed) {
        this.includeInFixed = includeInFixed;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }
}
