package com.example.mealsync.util;

public final class CutoffResult {
    private final String todayDate;
    private final String tomorrowDate;
    private final boolean isOpen;
    private final long millisUntilCutoff;

    public CutoffResult(String todayDate, String tomorrowDate, boolean isOpen, long millisUntilCutoff) {
        this.todayDate = todayDate;
        this.tomorrowDate = tomorrowDate;
        this.isOpen = isOpen;
        this.millisUntilCutoff = millisUntilCutoff;
    }

    public String getTodayDate() {
        return todayDate;
    }

    public String getTomorrowDate() {
        return tomorrowDate;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public long getMillisUntilCutoff() {
        return millisUntilCutoff;
    }
}
