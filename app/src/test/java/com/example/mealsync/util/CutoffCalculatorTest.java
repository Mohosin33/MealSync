package com.example.mealsync.util;

import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CutoffCalculatorTest {

    @Test
    public void testBeforeCutoff() {
        // 2025-05-10 20:00:00 in Asia/Dhaka (+06:00)
        ZonedDateTime dateTime = ZonedDateTime.of(2025, 5, 10, 20, 0, 0, 0, ZoneId.of("Asia/Dhaka"));
        Instant instant = dateTime.toInstant();

        CutoffResult result = CutoffCalculator.calculate(instant, "Asia/Dhaka", "21:00");

        assertEquals("2025-05-10", result.getTodayDate());
        assertEquals("2025-05-11", result.getTomorrowDate());
        assertTrue(result.isOpen());
        // 1 hour left = 3,600,000 ms
        assertEquals(3600000L, result.getMillisUntilCutoff());
    }

    @Test
    public void testExactlyAtCutoff() {
        // 2025-05-10 21:00:00 in Asia/Dhaka
        ZonedDateTime dateTime = ZonedDateTime.of(2025, 5, 10, 21, 0, 0, 0, ZoneId.of("Asia/Dhaka"));
        Instant instant = dateTime.toInstant();

        CutoffResult result = CutoffCalculator.calculate(instant, "Asia/Dhaka", "21:00");

        assertEquals("2025-05-10", result.getTodayDate());
        assertEquals("2025-05-11", result.getTomorrowDate());
        assertFalse(result.isOpen());
        assertEquals(0L, result.getMillisUntilCutoff());
    }

    @Test
    public void testAfterCutoff() {
        // 2025-05-10 21:01:00 in Asia/Dhaka
        ZonedDateTime dateTime = ZonedDateTime.of(2025, 5, 10, 21, 1, 0, 0, ZoneId.of("Asia/Dhaka"));
        Instant instant = dateTime.toInstant();

        CutoffResult result = CutoffCalculator.calculate(instant, "Asia/Dhaka", "21:00");

        assertEquals("2025-05-10", result.getTodayDate());
        assertEquals("2025-05-11", result.getTomorrowDate());
        assertFalse(result.isOpen());
        assertEquals(0L, result.getMillisUntilCutoff());
    }

    @Test
    public void testMidnightRollover() {
        // 2025-05-11 00:05:00 in Asia/Dhaka
        ZonedDateTime dateTime = ZonedDateTime.of(2025, 5, 11, 0, 5, 0, 0, ZoneId.of("Asia/Dhaka"));
        Instant instant = dateTime.toInstant();

        CutoffResult result = CutoffCalculator.calculate(instant, "Asia/Dhaka", "21:00");

        assertEquals("2025-05-11", result.getTodayDate());
        assertEquals("2025-05-12", result.getTomorrowDate());
        assertTrue(result.isOpen());
        // From 00:05 to 21:00 = 20h 55m = 75,300,000 ms
        assertEquals(75300000L, result.getMillisUntilCutoff());
    }

    @Test
    public void testTwoDifferentTimezones() {
        // Instant representing 2025-05-10 16:00:00 UTC
        Instant instant = Instant.parse("2025-05-10T16:00:00Z");

        // In Asia/Dhaka (+06:00), this instant is 2025-05-10 22:00:00 -> AFTER cutoff 21:00
        CutoffResult resultDhaka = CutoffCalculator.calculate(instant, "Asia/Dhaka", "21:00");
        assertEquals("2025-05-10", resultDhaka.getTodayDate());
        assertEquals("2025-05-11", resultDhaka.getTomorrowDate());
        assertFalse(resultDhaka.isOpen());

        // In America/New_York (-04:00), this instant is 2025-05-10 12:00:00 -> BEFORE cutoff 21:00
        CutoffResult resultNY = CutoffCalculator.calculate(instant, "America/New_York", "21:00");
        assertEquals("2025-05-10", resultNY.getTodayDate());
        assertEquals("2025-05-11", resultNY.getTomorrowDate());
        assertTrue(resultNY.isOpen());
        // From 12:00 to 21:00 = 9h = 32,400,000 ms
        assertEquals(32400000L, resultNY.getMillisUntilCutoff());
    }
}
