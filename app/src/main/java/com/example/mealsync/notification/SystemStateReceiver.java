package com.example.mealsync.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class SystemStateReceiver extends BroadcastReceiver {

    public interface Listener {
        void onAirplaneModeChanged(boolean enabled);
    }

    private final Listener listener;

    public SystemStateReceiver(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
            return;
        }
        boolean enabled = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        listener.onAirplaneModeChanged(enabled);
    }
}
