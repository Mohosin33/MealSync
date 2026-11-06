package com.example.mealsync.data.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Adjustment {
    private String id;
    private String type;       // "reversal" or "member_adjustment"
    private String targetType; // "expense" or "deposit" (for reversal)
    private String targetId;   // for reversal
    private String memberId;   // for member_adjustment
    private long amountPaisa;  // signed non-zero for member_adjustment
    private String reason;
    private String periodId;
    private String createdBy;
    @ServerTimestamp
    private Date createdAt;

    public Adjustment() {
    }

    public static Adjustment createReversal(String id, String targetType, String targetId, String reason, String periodId, String createdBy) {
        return new Adjustment(id, "reversal", targetType, targetId, null, 0, reason, periodId, createdBy);
    }

    public static Adjustment createMemberAdjustment(String id, String memberId, long amountPaisa, String reason, String periodId, String createdBy) {
        return new Adjustment(id, "member_adjustment", null, null, memberId, amountPaisa, reason, periodId, createdBy);
    }

    public Adjustment(String id, String type, String targetType, String targetId,
                      String memberId, long amountPaisa, String reason, String periodId, String createdBy) {
        this.id = id;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.memberId = memberId;
        this.amountPaisa = amountPaisa;
        this.reason = reason;
        this.periodId = periodId;
        this.createdBy = createdBy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public long getAmountPaisa() {
        return amountPaisa;
    }

    public void setAmountPaisa(long amountPaisa) {
        this.amountPaisa = amountPaisa;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPeriodId() {
        return periodId;
    }

    public void setPeriodId(String periodId) {
        this.periodId = periodId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
