package com.example.mealsync.data.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Deposit {
    private String id;
    private String memberId;
    private long amountPaisa;
    private String method; // "cash", "bank", "bkash", "nagad", "other"
    private String description;
    private String periodId;
    private String createdBy;
    private String correctsId;
    @ServerTimestamp
    private Date createdAt;

    public Deposit() {
    }

    public Deposit(String id, String memberId, long amountPaisa, String method,
                   String description, String periodId, String createdBy) {
        this(id, memberId, amountPaisa, method, description, periodId, createdBy, null);
    }

    public Deposit(String id, String memberId, long amountPaisa, String method,
                   String description, String periodId, String createdBy, String correctsId) {
        this.id = id;
        this.memberId = memberId;
        this.amountPaisa = amountPaisa;
        this.method = method;
        this.description = description;
        this.periodId = periodId;
        this.createdBy = createdBy;
        this.correctsId = correctsId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCorrectsId() {
        return correctsId;
    }

    public void setCorrectsId(String correctsId) {
        this.correctsId = correctsId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
