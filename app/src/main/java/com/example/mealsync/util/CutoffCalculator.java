package com.example.mealsync.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class CutoffCalculator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private CutoffCalculator() {
    }

    public static CutoffResult calculate(Instant currentInstant, String timezoneStr, String cutoffTimeStr) {
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezoneStr);
        } catch (Exception e) {
            zoneId = ZoneId.of("Asia/Dhaka");
        }

        LocalTime cutoffTime;
        try {
            cutoffTime = LocalTime.parse(cutoffTimeStr != null ? cutoffTimeStr.trim() : "21:00", TIME_FORMATTER);
        } catch (Exception e) {
            cutoffTime = LocalTime.of(21, 0);
        }

        ZonedDateTime nowZoned = currentInstant.atZone(zoneId);
        LocalDate today = nowZoned.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);

        String todayDateStr = today.format(DATE_FORMATTER);
        String tomorrowDateStr = tomorrow.format(DATE_FORMATTER);

        ZonedDateTime cutoffToday = ZonedDateTime.of(today, cutoffTime, zoneId);

        boolean isOpen = nowZoned.isBefore(cutoffToday);
        long millisUntilCutoff = 0;
        if (isOpen) {
            millisUntilCutoff = Duration.between(nowZoned, cutoffToday).toMillis();
        }

        return new CutoffResult(todayDateStr, tomorrowDateStr, isOpen, millisUntilCutoff);
    }
}
