package com.example.mealsync.data.models;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Mess {
    private String messId;
    private String name;
    private String managerUid;
    private String managerName;
    private String timezone;
    private String cutoffTime;
    private String invitationCode;
    private int utcOffsetMinutes;
    private String currentPeriodId;
    private long mealRatePaisa;
    private int guestWeightPercent = 100;
    private String fixedSplitMode = "equal";
    private long createdAt;

    public Mess() {
    }

    public Mess(String messId, String name, String managerUid, String managerName,
                String timezone, String cutoffTime, String invitationCode, int utcOffsetMinutes,
                String currentPeriodId, int guestWeightPercent, String fixedSplitMode, long createdAt) {
        this.messId = messId;
        this.name = name;
        this.managerUid = managerUid;
        this.managerName = managerName;
        this.timezone = timezone;
        this.cutoffTime = cutoffTime;
        this.invitationCode = invitationCode;
        this.utcOffsetMinutes = utcOffsetMinutes;
        this.currentPeriodId = currentPeriodId;
        this.guestWeightPercent = guestWeightPercent;
        this.fixedSplitMode = fixedSplitMode;
        this.createdAt = createdAt;
    }

    public static int calculateUtcOffsetMinutes(String timezoneStr) {
        try {
            ZoneId zoneId = ZoneId.of(timezoneStr != null ? timezoneStr.trim() : "Asia/Dhaka");
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            return now.getOffset().getTotalSeconds() / 60;
        } catch (Exception e) {
            return 360; // Default Asia/Dhaka (+06:00)
        }
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

    public int getUtcOffsetMinutes() {
        return utcOffsetMinutes;
    }

    public void setUtcOffsetMinutes(int utcOffsetMinutes) {
        this.utcOffsetMinutes = utcOffsetMinutes;
    }

    public String getCurrentPeriodId() {
        return currentPeriodId;
    }

    public void setCurrentPeriodId(String currentPeriodId) {
        this.currentPeriodId = currentPeriodId;
    }

    public long getMealRatePaisa() {
        return mealRatePaisa;
    }

    public void setMealRatePaisa(long mealRatePaisa) {
        this.mealRatePaisa = mealRatePaisa;
    }

    public int getGuestWeightPercent() {
        return guestWeightPercent;
    }

    public void setGuestWeightPercent(int guestWeightPercent) {
        this.guestWeightPercent = guestWeightPercent;
    }

    public String getFixedSplitMode() {
        return fixedSplitMode;
    }

    public void setFixedSplitMode(String fixedSplitMode) {
        this.fixedSplitMode = fixedSplitMode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
