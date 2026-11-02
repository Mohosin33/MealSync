package com.example.mealsync.data.models;

public class Mess {
    private String messId;
    private String name;
    private String managerUid;
    private String managerName;
    private String timezone;
    private String cutoffTime;
    private String invitationCode;
    private long createdAt;

    public Mess() {
    }

    public Mess(String messId, String name, String managerUid, String managerName,
                String timezone, String cutoffTime, String invitationCode, long createdAt) {
        this.messId = messId;
        this.name = name;
        this.managerUid = managerUid;
        this.managerName = managerName;
        this.timezone = timezone;
        this.cutoffTime = cutoffTime;
        this.invitationCode = invitationCode;
        this.createdAt = createdAt;
    }

    public String getMessId() {
        return messId;
    }

    public void setMessId(String messId) {
        this.messId = messId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManagerUid() {
        return managerUid;
    }

    public void setManagerUid(String managerUid) {
        this.managerUid = managerUid;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCutoffTime() {
        return cutoffTime;
    }

    public void setCutoffTime(String cutoffTime) {
        this.cutoffTime = cutoffTime;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
