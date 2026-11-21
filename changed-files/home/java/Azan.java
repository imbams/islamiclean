package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The call to prayer at each of the day's prayer times (PrayerDay): the azan band over whatever app
 * is in front ({@link AzanOverlay}), with azan.mp3 (azan-subuh.mp3 for Subuh) from the theme folder
 * when it has one.
 *
 * <p>Only the first window asks: {@link #takeDue} marks that prayer given, so a second check a few
 * seconds later does not start it again. The alarm ({@link AzanAlarm}) is what wakes the launcher
 * while another app is in front.
 */
final class Azan {
    private static final String TAG = "TvHome.Azan";
    /** Minutes after the prayer time in which the azan still starts, so a late tick does not miss it. */
    private static final int WINDOW_MIN = 3;
    /** Marks, in the launcher's preferences, the prayer whose azan has already been given. */
    private static final String PREFS = "azan", GIVEN = "given";

    private Azan() {
    }

    /**
     * The prayer whose azan is due now and has not been given yet, or -1. Marks it given, so a later
     * check (the alarm and the screensaver, say) does not start it again.
     */
    static int takeDue(Context ctx) {
        if (!PrayerDay.azanEnabled(ctx)) return -1;
        LocalDate today = LocalDate.now();
        int[] times = PrayerDay.times(ctx, today);
        LocalTime now = LocalTime.now();
        int nowMin = now.getHour() * 60 + now.getMinute();

        // To try it out: touch azan-now in the theme folder, and the next prayer's azan starts at
        // the next check.
        File dir = Config.dir(ctx);
        File test = dir != null ? new File(dir, "azan-now") : null;
        if (test != null && test.delete()) {
            int next = PrayerTimes.next(times, now.toSecondOfDay());
            return next;
        }

        for (int i = 0; i < times.length; i++) {
            if (nowMin < times[i] || nowMin >= times[i] + WINDOW_MIN) continue;
            String key = today + "#" + i;
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (key.equals(p.getString(GIVEN, ""))) return -1;
            p.edit().putString(GIVEN, key).commit();
            return i;
        }
        return -1;
    }

    /** azan-subuh.mp3 for Subuh when the theme has it, otherwise azan.mp3, otherwise null. */
    static File sound(Context ctx, int index) {
        File dir = Config.dir(ctx);
        if (dir == null) return null;
        if (index == PrayerTimes.SUBUH) {
            File subuh = new File(dir, "azan-subuh.mp3");
            if (subuh.isFile() && subuh.length() > 0) return subuh;
        }
        File azan = new File(dir, "azan.mp3");
        return azan.isFile() && azan.length() > 0 ? azan : null;
    }
}
