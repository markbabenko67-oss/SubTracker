package com.subtracker.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1);
        if (id < 0) {
            return;
        }
        SubscriptionDbHelper db = new SubscriptionDbHelper(context);
        Subscription s = db.get(id);
        if (s == null) {
            return;
        }

        ReminderScheduler.ensureChannel(context);

        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, (int) id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "Сегодня списание";
        String text = s.name + " — " + SubscriptionAdapter.formatMoney(s.price) + " ₽";

        Notification notification = new Notification.Builder(context,
                ReminderScheduler.getChannelId())
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        nm.notify((int) id, notification);

        ReminderScheduler.scheduleOne(context, s, db);
    }
}