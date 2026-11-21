package com.gathin.tvhome;

import android.content.res.Resources;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The Javanese day and pasaran (the five-day week) for a Gregorian date. A small arithmetic cycle
 * instead of a table: 1945-08-17, a Jumat Legi, anchors pasaran, and the weekday comes from the
 * locale so the line reads "Kamis Pon" in Indonesian.
 */
final class Weton {

    /** The pasaran cycle in order; 1945-08-17 (the first Jumat Legi of the republic) is the anchor. */
    private static final String[] PASARAN = {"Legi", "Pahing", "Pon", "Wage", "Kliwon"};
    private static final long ANCHOR_EPOCH_DAY = LocalDate.of(1945, 8, 17).toEpochDay();

    private Weton() {
    }

    /** e.g. "Kamis Pon": the locale's weekday and the pasaran, for the line above the date. */
    static String today(Resources res) {
        LocalDate date = LocalDate.now();
        Locale locale = res.getConfiguration().getLocales().get(0);
        String day = date.format(DateTimeFormatter.ofPattern("EEEE", locale));
        return res.getString(R.string.weton, day, pasaran(date));
    }

    /** "Legi", "Pahing", "Pon", "Wage" or "Kliwon". */
    static String pasaran(LocalDate date) {
        long days = date.toEpochDay() - ANCHOR_EPOCH_DAY;
        return PASARAN[(int) Math.floorMod(days, (long) PASARAN.length)];
    }
}
