package com.example.mealsync.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public final class ClockCheckLogic {

    private ClockCheckLogic() {
    }

    public static long parseNetworkTimeMillis(String json) throws JSONException {
        JSONObject response = new JSONObject(json);
        if (!"UTC".equals(response.optString("timeZone"))
                || !response.has("dateTime")
                || response.isNull("dateTime")) {
            throw new JSONException("Time response does not contain a UTC timestamp");
        }
        try {
            return LocalDateTime.parse(response.getString("dateTime"))
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli();
        } catch (DateTimeParseException error) {
            throw new JSONException("Time response contains an invalid timestamp", error);
        }
    }

    public static long differenceMillis(long networkTimeMillis, long phoneTimeMillis) {
        return networkTimeMillis - phoneTimeMillis;
    }

    public static boolean isClockOffByMoreThanTwoMinutes(long differenceMillis) {
        return differenceMillis > 120_000L || differenceMillis < -120_000L;
    }
}
