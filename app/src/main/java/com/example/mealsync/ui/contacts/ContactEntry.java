package com.example.mealsync.ui.contacts;

public final class ContactEntry {

    private final String displayName;
    private final String phoneNumber;

    public ContactEntry(String displayName, String phoneNumber) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
