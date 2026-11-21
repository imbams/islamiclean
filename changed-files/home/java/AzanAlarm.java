package com.gathin.tvhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Wakes the launcher at the next prayer time, so the azan comes even while another app (YouTube,
 * Netflix, Live TV) is in front: then {@link AzanOverlay} shows it over that app.
 *
 * <p>Set again whenever Home starts, after every alarm, and when the TV boots. Without the
 * exact-alarm permission the alarm is a minute-window one instead of exact.
 */
final class AzanAlarm {
    private static final String TAG = "TvHome.Azan";
    static final String ACTION = "com.gathin.tvhome.AZAN";

    private AzanAlarm() {
    }

    /** Sets the alarm for the next prayer time after now; clears it when the azan is off. */
    static void scheduleNext(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0,
                new Intent(ACTION).setClass(ctx, Receiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long at = PrayerDay.azanEnabled(ctx) ? next(ctx) : -1;
        if (at < 0) {
            am.cancel(pi);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Without the exact-alarm permission (Android 12+), within a minute of the time.
            am.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        Log.i(TAG, "next azan alarm at " + new java.util.Date(at));
    }

    /** The next prayer time after now, today or tomorrow, a second past the minute; -1 when unknown. */
    private static long next(Context ctx) {
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now();
        for (int day = 0; day < 2; day++) {
            LocalDate date = today.plusDays(day);
            int[] times = PrayerDay.times(ctx, date);
            for (int m : times) {
                ZonedDateTime t = ZonedDateTime.of(date, LocalTime.of(m / 60, m % 60, 1), zone);
                if (t.isAfter(now)) return t.toInstant().toEpochMilli();
            }
        }
        return -1;
    }

    /** The alarm itself, and the TV starting up or the launcher being updated. */
    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (ACTION.equals(action)) {
                PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                boolean screenOn = pm == null || pm.isInteractive();
                // With the screen off nobody hears it; the next prayer is scheduled either way.
                if (screenOn && PrayerDay.azanEnabled(ctx)) {
                    Intent svc = new Intent(ctx, AzanOverlay.class);
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ctx.startForegroundService(svc);
                        } else {
                            ctx.startService(svc);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "cannot start the azan overlay: " + e.getMessage());
                    }
                }
            }
            scheduleNext(ctx);
        }
    }
}
