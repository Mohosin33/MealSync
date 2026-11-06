package com.example.mealsync.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtils {

    private MoneyUtils() {
    }

    /**
     * Parses Taka string to Paisa (1 Taka = 100 Paisa).
     * Accepts "12", "12.5" and "12.50".
     * Rejects negatives, zero, letters, more than 2 decimals, and anything above 1,000,000 Taka.
     */
    public static long parseTakaToPaisa(String takaText) throws IllegalArgumentException {
        if (takaText == null) {
            throw new IllegalArgumentException("Taka text cannot be null");
        }
        String s = takaText.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Taka text cannot be empty");
        }
        if (s.startsWith("-")) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        String[] parts = s.split("\\.", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("Invalid decimal format");
        }

        String takaPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "";

        if (takaPart.isEmpty() || !takaPart.matches("\\d+")) {
            throw new IllegalArgumentException("Taka part must contain digits");
        }
        if (!decimalPart.isEmpty() && !decimalPart.matches("\\d+")) {
            throw new IllegalArgumentException("Decimal part must contain digits");
        }
        if (decimalPart.length() > 2) {
            throw new IllegalArgumentException("More than 2 decimal places not allowed");
        }

        long taka = Long.parseLong(takaPart);
        long paisa = 0;
        if (decimalPart.length() == 1) {
            paisa = Long.parseLong(decimalPart) * 10;
        } else if (decimalPart.length() == 2) {
            paisa = Long.parseLong(decimalPart);
        }

        long totalPaisa = taka * 100 + paisa;
        if (totalPaisa <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (totalPaisa > 100_000_000L) { // 1,000,000 Taka = 100,000,000 Paisa
            throw new IllegalArgumentException("Amount exceeds maximum allowed (1,000,000 Taka)");
        }

        return totalPaisa;
    }

    /**
     * Formats Paisa as "৳1,234.50" or "-৳1,234.50".
     */
    public static String formatPaisa(long paisa) {
        boolean negative = paisa < 0;
        long absPaisa = Math.abs(paisa);
        long taka = absPaisa / 100;
        long cents = absPaisa % 100;

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        String takaStr = nf.format(taka);
        String formatted = String.format(Locale.US, "৳%s.%02d", takaStr, cents);
        return negative ? "-" + formatted : formatted;
    }
}
