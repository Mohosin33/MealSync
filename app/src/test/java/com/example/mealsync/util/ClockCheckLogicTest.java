package com.example.mealsync.util;

import org.json.JSONException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClockCheckLogicTest {

    @Test
    public void parsesUtcDateTimeWithFractionalSeconds() throws JSONException {
        assertEquals(1_794_049_228_849L,
                ClockCheckLogic.parseNetworkTimeMillis(
                        "{\"timeZone\":\"UTC\",\"dateTime\":\"2026-11-07T11:00:28.8499124\"}"));
    }

    @Test(expected = JSONException.class)
    public void rejectsResponseWithoutUtcTimezone() throws JSONException {
        ClockCheckLogic.parseNetworkTimeMillis(
                "{\"dateTime\":\"2026-11-07T11:00:28.8499124\"}");
    }

    @Test(expected = JSONException.class)
    public void rejectsMalformedDateTime() throws JSONException {
        ClockCheckLogic.parseNetworkTimeMillis(
                "{\"timeZone\":\"UTC\",\"dateTime\":\"not-a-time\"}");
    }

    @Test
    public void differenceIsNetworkTimeMinusPhoneTime() {
        assertEquals(3_000L, ClockCheckLogic.differenceMillis(13_000L, 10_000L));
        assertEquals(-3_000L, ClockCheckLogic.differenceMillis(10_000L, 13_000L));
    }

    @Test
    public void warnsOnlyWhenDifferenceExceedsTwoMinutes() {
        assertFalse(ClockCheckLogic.isClockOffByMoreThanTwoMinutes(120_000L));
        assertFalse(ClockCheckLogic.isClockOffByMoreThanTwoMinutes(-120_000L));
        assertTrue(ClockCheckLogic.isClockOffByMoreThanTwoMinutes(120_001L));
        assertTrue(ClockCheckLogic.isClockOffByMoreThanTwoMinutes(-120_001L));
    }
}
