package com.subtracker.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.time.ZonedDateTime;
import java.util.List;

public class ReminderScheduler {

    private static final String CHANNEL_ID = "charge_reminders";
    public static final String EXTRA_ID = "subscription_id";

    private ReminderScheduler() {}

    public static String getChannelId() {
        return CHANNEL_ID;
    }

    public static void ensureChannel(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL_ID) == null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "Напоминания о списаниях",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Уведомления о предстоящих списаниях подписок");
            nm.createNotificationChannel(ch);
        }
    }

    public static void rescheduleAll(Context context) {
        SubscriptionDbHelper db = new SubscriptionDbHelper(context);
        List<Subscription> all = db.getAll();
        for (Subscription s : all) {
            scheduleOne(context, s, db);
        }
    }

    public static void scheduleOne(Context context, Subscription s) {
        scheduleOne(context, s, new SubscriptionDbHelper(context));
    }

    public static void scheduleOne(Context context, Subscription s, SubscriptionDbHelper db) {
        ZonedDateTime next = Subscription.nextOccurrence(s.day,
                System.currentTimeMillis(), s.period);
        s.nextCharge = next.toInstant().toEpochMilli();
        db.update(s);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class)
                .putExtra(EXTRA_ID, s.id);
        PendingIntent pi = PendingIntent.getBroadcast(context, (int) s.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAt = s.nextCharge;
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } catch (SecurityException e) {
            Log.w("ReminderScheduler", "setAndAllowWhileIdle rejected", e);
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancel(Context context, long id) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, (int) id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }
}