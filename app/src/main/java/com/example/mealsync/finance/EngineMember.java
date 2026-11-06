package com.example.mealsync.finance;

public class EngineMember {
    private final String uid;
    private final boolean active;
    private final boolean includeInFixed;
    private final Long joinedAt;
    private final Long leftAt;

    public EngineMember(String uid, boolean active, boolean includeInFixed) {
        this(uid, active, includeInFixed, null, null);
    }

    public EngineMember(String uid, boolean active, boolean includeInFixed, Long joinedAt, Long leftAt) {
        this.uid = uid;
        this.active = active;
        this.includeInFixed = includeInFixed;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
    }

    public String getUid() {
        return uid;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isIncludeInFixed() {
        return includeInFixed;
    }

    public Long getJoinedAt() {
        return joinedAt;
    }

    public Long getLeftAt() {
        return leftAt;
    }
}
