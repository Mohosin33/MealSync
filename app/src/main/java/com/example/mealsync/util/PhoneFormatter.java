package com.example.mealsync.util;

/**
 * Converts what the user typed into an international (E.164) phone number.
 * Pure Java with no Android imports, so it can be unit tested.
 * Default country: Bangladesh (+880). Other countries must start with + or 00.
 */
public final class PhoneFormatter {

    private PhoneFormatter() {
    }

    /** Returns the E.164 number (for example +8801700000000) or null if it is invalid. */
    public static String toE164(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replaceAll("[\\s\\-()]", "");
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("00")) {
            s = "+" + s.substring(2);
        }
        if (s.startsWith("+")) {
            return s.matches("\\+[1-9]\\d{7,14}") ? s : null;
        }
        // Bangladesh mobile numbers: 013 to 019 followed by 8 digits.
        if (s.matches("01[3-9]\\d{8}")) {
            return "+880" + s.substring(1);
        }
        if (s.matches("8801[3-9]\\d{8}")) {
            return "+" + s;
        }
        if (s.matches("1[3-9]\\d{8}")) {
            return "+880" + s;
        }
        return null;
    }
}