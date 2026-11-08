package com.example.mealsync.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.mealsync.data.models.Mess;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;

public final class MealReminderScheduler {

    static final String ACTION_MEAL_REMINDER =
            "com.example.mealsync.action.MEAL_CUTOFF_REMINDER";
    static final String PREFS_NAME = "meal_reminder";
    static final String KEY_USER_ID = "user_id";
    static final String KEY_MESS_ID = "mess_id";
    static final String KEY_MESS_NAME = "mess_name";
    static final String KEY_TIMEZONE = "timezone";
    static final String KEY_CUTOFF_TIME = "cutoff_time";
    private static final String TAG = "MealReminder";
    private static final int REQUEST_CODE = 1001;

    private MealReminderScheduler() {
    }

    public static void schedule(Context context, String userId, Mess mess) {
        if (userId == null || userId.trim().isEmpty() || mess == null
                || mess.getMessId() == null || mess.getMessId().trim().isEmpty()) {
            cancel(context);
            return;
        }

        String timezone = mess.getTimezone();
        String cutoffTime = mess.getCutoffTime();
        long triggerAtMillis;
        try {
            if (timezone == null || cutoffTime == null) {
                throw new DateTimeException("Mess timezone or cutoff time is missing");
            }
            ZoneId zoneId = ZoneId.of(timezone);
            LocalTime cutoff = LocalTime.parse(cutoffTime, DateTimeFormatter.ofPattern("HH:mm"));
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            ZonedDateTime nextReminder = LocalDate.now(zoneId)
                    .atTime(cutoff.minusMinutes(10))
                    .atZone(zoneId);
            if (!nextReminder.isAfter(now)) {
                nextReminder = nextReminder.plusDays(1);
            }
            triggerAtMillis = nextReminder.toInstant().toEpochMilli();
        } catch (DateTimeException error) {
            Log.e(TAG, "Cannot schedule meal reminder with the mess cutoff or timezone", error);
            cancel(context);
            return;
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_MESS_ID, mess.getMessId())
                .putString(KEY_MESS_NAME, mess.getName())
                .putString(KEY_TIMEZONE, timezone)
                .putString(KEY_CUTOFF_TIME, cutoffTime)
                .apply();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(context);
        if (alarmManager == null || pendingIntent == null) {
            Log.e(TAG, "Could not schedule meal reminder: alarm service unavailable");
            return;
        }
        alarmManager.cancel(pendingIntent);
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    static void scheduleFromSavedSettings(Context context) {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(KEY_USER_ID, null);
        String messId = prefs.getString(KEY_MESS_ID, null);
        String timezone = prefs.getString(KEY_TIMEZONE, null);
        String cutoffTime = prefs.getString(KEY_CUTOFF_TIME, null);
        if (userId == null || messId == null || timezone == null || cutoffTime == null) {
            return;
        }

        Mess savedMess = new Mess();
        savedMess.setMessId(messId);
        savedMess.setName(prefs.getString(KEY_MESS_NAME, ""));
        savedMess.setTimezone(timezone);
        savedMess.setCutoffTime(cutoffTime);
        schedule(context, userId, savedMess);
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(context);
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, MealReminderReceiver.class)
                .setAction(ACTION_MEAL_REMINDER);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
