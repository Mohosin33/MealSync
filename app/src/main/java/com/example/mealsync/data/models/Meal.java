package com.example.mealsync.data.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Meal {
    private String date;
    private String uid;
    private boolean lunch;
    private boolean dinner;
    private int guests;
    @ServerTimestamp
    private Date updatedAt;

    @Exclude
    private boolean pending;

    public Meal() {
    }

    public Meal(String date, String uid, boolean lunch, boolean dinner, int guests) {
        this.date = date;
        this.uid = uid;
        this.lunch = lunch;
        this.dinner = dinner;
        this.guests = guests;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public boolean isLunch() {
        return lunch;
    }

    public void setLunch(boolean lunch) {
        this.lunch = lunch;
    }

    public boolean isDinner() {
        return dinner;
    }

    public void setDinner(boolean dinner) {
        this.dinner = dinner;
    }

    public int getGuests() {
        return guests;
    }

    public void setGuests(int guests) {
        this.guests = guests;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Exclude
    public boolean isPending() {
        return pending;
    }

    @Exclude
    public void setPending(boolean pending) {
        this.pending = pending;
    }
}
