package com.example.mealsync.finance;

public class EngineMeal {
    private final String uid;
    private final int lunches;
    private final int dinners;
    private final int guests;

    public EngineMeal(String uid, int lunches, int dinners, int guests) {
        this.uid = uid;
        this.lunches = lunches;
        this.dinners = dinners;
        this.guests = guests;
    }

    public String getUid() {
        return uid;
    }

    public int getLunches() {
        return lunches;
    }

    public int getDinners() {
        return dinners;
    }

    public int getGuests() {
        return guests;
    }
}
