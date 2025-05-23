package hu.mobilalk.gazorabejelentes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.showReadingReminderNotification();
    }
}