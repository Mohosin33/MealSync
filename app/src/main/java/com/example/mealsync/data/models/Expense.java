package com.example.mealsync.data.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Expense {
    private String id;
    private long amountPaisa;
    private String description;
    private String category;
    private String type; // Legacy variable expenses remain readable; new expenses are fixed.
    private String expenseDate; // "yyyy-MM-dd"
    private String periodId; // "yyyy-MM"
    private String createdBy;
    private String correctsId;
    @ServerTimestamp
    private Date createdAt;

    public Expense() {
    }

    public Expense(String id, long amountPaisa, String description, String category,
                   String type, String expenseDate, String periodId, String createdBy) {
        this(id, amountPaisa, description, category, type, expenseDate, periodId, createdBy, null);
    }

    public Expense(String id, long amountPaisa, String description, String category,
                   String type, String expenseDate, String periodId, String createdBy, String correctsId) {
        this.id = id;
        this.amountPaisa = amountPaisa;
        this.description = description;
        this.category = category;
        this.type = type;
        this.expenseDate = expenseDate;
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

    public long getAmountPaisa() {
        return amountPaisa;
    }

    public void setAmountPaisa(long amountPaisa) {
        this.amountPaisa = amountPaisa;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(String expenseDate) {
        this.expenseDate = expenseDate;
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
