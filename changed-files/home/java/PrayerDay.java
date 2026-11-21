package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Resources;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Today's prayer times for the place in config.xml, shared by the azan (Azan, AzanAlarm,
 * AzanOverlay) and the prayer screensaver (PrayerScene). PrayerTimes itself stays pure arithmetic;
 * this reads the coordinates, the Subuh adjustment and the on/off switches from resources.
 */
final class PrayerDay {

    private PrayerDay() {
    }

    /** show_prayer_times: the panel on Home, and the times the azan and the screensaver use. */
    static boolean enabled(Context ctx) {
        return ctx.getResources().getBoolean(R.bool.show_prayer_times);
    }

    /** The azan is on when prayer times are on and the azan switch is too. */
    static boolean azanEnabled(Context ctx) {
        return enabled(ctx) && ctx.getResources().getBoolean(R.bool.azan);
    }

    /** Subuh, Dzuhur, Ashar, Maghrib and Isya today, as minutes after midnight. */
    static int[] times(Context ctx) {
        return times(ctx, LocalDate.now());
    }

    /** Subuh, Dzuhur, Ashar, Maghrib and Isya on {@code date}, as minutes after midnight. */
    static int[] times(Context ctx, LocalDate date) {
        Resources res = ctx.getResources();
        return PrayerTimes.forDate(date,
                decimal(res.getString(R.string.prayer_latitude), -7.8856),
                decimal(res.getString(R.string.prayer_longitude), 110.3289),
                ZoneId.systemDefault(), res.getInteger(R.integer.prayer_subuh_add));
    }

    /** The localized name of prayer {@code index}. */
    static String name(Context ctx, int index) {
        switch (index) {
            case PrayerTimes.DZUHUR:
                return ctx.getString(R.string.prayer_dzuhur);
            case PrayerTimes.ASHAR:
                return ctx.getString(R.string.prayer_ashar);
            case PrayerTimes.MAGHRIB:
                return ctx.getString(R.string.prayer_maghrib);
            case PrayerTimes.ISYA:
                return ctx.getString(R.string.prayer_isya);
            default:
                return ctx.getString(R.string.prayer_subuh);
        }
    }

    /** Minutes after midnight as the 24-hour HH:mm Kemenag's tables use. */
    static String clock(int minutes) {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    /** The KHGT Hijri date, or an empty string outside the table. */
    static String hijri() {
        String label = Hijri.dateLabel(LocalDate.now());
        return label != null ? label : "";
    }

    private static double decimal(String value, double fallback) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
