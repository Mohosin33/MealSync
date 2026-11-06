package com.example.mealsync.data.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.Map;

public class Period {
    private String periodId; // "yyyy-MM"
    private String status;   // "open", "closing", "closed"
    private String startDate; // "yyyy-MM-01"
    private String endDate;   // "yyyy-MM-dd"
    private Map<String, Long> openingBalances;
    private String closeRequestId;
    @ServerTimestamp
    private Date closingStartedAt;
    @ServerTimestamp
    private Date closedAt;
    private String closedBy;
    private Long finalRatePaisa;
    private Map<String, Long> totals;
    private Map<String, Long> unallocated;
    private String nextPeriodId;
    private String engineVersion;
    @ServerTimestamp
    private Date createdAt;

    public Period() {
    }

    public Period(String periodId, String status, String startDate, String endDate, Map<String, Long> openingBalances) {
        this.periodId = periodId;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.openingBalances = openingBalances;
    }

    public String getPeriodId() {
        return periodId;
    }

    public void setPeriodId(String periodId) {
        this.periodId = periodId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Map<String, Long> getOpeningBalances() {
        return openingBalances;
    }

    public void setOpeningBalances(Map<String, Long> openingBalances) {
        this.openingBalances = openingBalances;
    }

    public String getCloseRequestId() {
        return closeRequestId;
    }

    public void setCloseRequestId(String closeRequestId) {
        this.closeRequestId = closeRequestId;
    }

    public Date getClosingStartedAt() {
        return closingStartedAt;
    }

    public void setClosingStartedAt(Date closingStartedAt) {
        this.closingStartedAt = closingStartedAt;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public Long getFinalRatePaisa() {
        return finalRatePaisa;
    }

    public void setFinalRatePaisa(Long finalRatePaisa) {
        this.finalRatePaisa = finalRatePaisa;
    }

    public Map<String, Long> getTotals() {
        return totals;
    }

    public void setTotals(Map<String, Long> totals) {
        this.totals = totals;
    }

    public Map<String, Long> getUnallocated() {
        return unallocated;
    }

    public void setUnallocated(Map<String, Long> unallocated) {
        this.unallocated = unallocated;
    }

    public String getNextPeriodId() {
        return nextPeriodId;
    }

    public void setNextPeriodId(String nextPeriodId) {
        this.nextPeriodId = nextPeriodId;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
