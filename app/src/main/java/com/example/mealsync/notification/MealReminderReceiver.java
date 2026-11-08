package com.example.mealsync.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.mealsync.MainActivity;
import com.example.mealsync.R;
import com.google.firebase.auth.FirebaseAuth;

public class MealReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "MealReminder";
    private static final String CHANNEL_ID = "meal_cutoff_reminders";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !MealReminderScheduler.ACTION_MEAL_REMINDER.equals(intent.getAction())) {
            return;
        }

        android.content.SharedPreferences prefs = context.getSharedPreferences(
                MealReminderScheduler.PREFS_NAME, Context.MODE_PRIVATE);
        String reminderUserId = prefs.getString(MealReminderScheduler.KEY_USER_ID, null);
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : null;
        if (reminderUserId == null || !reminderUserId.equals(currentUserId)) {
            MealReminderScheduler.cancel(context);
            return;
        }

        if (hasNotificationPermission(context)) {
            showNotification(context, prefs.getString(MealReminderScheduler.KEY_MESS_NAME, ""));
        }
        MealReminderScheduler.scheduleFromSavedSettings(context);
    }

    private boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showNotification(Context context, String messName) {
        createNotificationChannel(context);
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, NOTIFICATION_ID,
                openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = context.getString(R.string.meal_reminder_title);
        String message = messName == null || messName.trim().isEmpty()
                ? context.getString(R.string.meal_reminder_message)
                : context.getString(R.string.meal_reminder_message_for_mess, messName);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification.build());
        } catch (SecurityException error) {
            Log.w(TAG, "Notification permission is not available", error);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.meal_reminder_channel),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.meal_reminder_channel_description));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
