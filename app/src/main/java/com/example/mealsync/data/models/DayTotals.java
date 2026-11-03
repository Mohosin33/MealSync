package com.example.mealsync.data.models;

public class DayTotals {
    private final int totalLunch;
    private final int totalDinner;
    private final int totalGuests;

    public DayTotals(int totalLunch, int totalDinner, int totalGuests) {
        this.totalLunch = totalLunch;
        this.totalDinner = totalDinner;
        this.totalGuests = totalGuests;
    }

    public int getTotalLunch() {
        return totalLunch;
    }

    public int getTotalDinner() {
        return totalDinner;
    }

    public int getTotalGuests() {
        return totalGuests;
    }

    public int getGrandTotal() {
        return totalLunch + totalDinner + totalGuests;
    }
}
