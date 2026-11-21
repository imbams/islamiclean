package com.gathin.tvhome;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Hijri dates from Muhammadiyah's KHGT (Kalender Hijriah Global Tunggal), the single global calendar
 * Majelis Tarjih dan Tajdid publishes at khgt.muhammadiyah.or.id. The month starts are tabulated rather
 * than computed: KHGT follows the global crescent, so its months do not always follow the arithmetic
 * (tabular) Islamic calendar by a day. The table runs from Muharam 1447 to Zulhijah 1454; outside it the
 * panel says nothing instead of showing a date it cannot vouch for.
 */
final class Hijri {

    private Hijri() {
    }

    static final int MUHARAM = 1;
    static final int RAMADAN = 9;
    static final int SYAWAL = 10;
    static final int ZULHIJAH = 12;

    private static final int FIRST_YEAR = 1447;

    private static final String[] MONTHS = {
            "Muharam", "Safar", "Rabiulawal", "Rabiulakhir", "Jumadilawal", "Jumadilakhir",
            "Rajab", "Syakban", "Ramadan", "Syawal", "Zulkaidah", "Zulhijah",
    };

    /** First day of each month, year by year from FIRST_YEAR: 1447, then 1448, and so on. */
    private static final String[] STARTS = {
            // 1447
            "2025-06-26", "2025-07-26", "2025-08-24", "2025-09-23", "2025-10-23", "2025-11-21",
            "2025-12-21", "2026-01-20", "2026-02-18", "2026-03-20", "2026-04-18", "2026-05-18",
            // 1448
            "2026-06-16", "2026-07-15", "2026-08-14", "2026-09-12", "2026-10-12", "2026-11-10",
            "2026-12-10", "2027-01-09", "2027-02-08", "2027-03-09", "2027-04-08", "2027-05-07",
            // 1449
            "2027-06-06", "2027-07-05", "2027-08-03", "2027-09-02", "2027-10-01", "2027-10-31",
            "2027-11-29", "2027-12-29", "2028-01-28", "2028-02-26", "2028-03-27", "2028-04-26",
            // 1450
            "2028-05-25", "2028-06-24", "2028-07-23", "2028-08-21", "2028-09-20", "2028-10-19",
            "2028-11-17", "2028-12-17", "2029-01-16", "2029-02-14", "2029-03-16", "2029-04-15",
            // 1451
            "2029-05-15", "2029-06-13", "2029-07-13", "2029-08-11", "2029-09-09", "2029-10-09",
            "2029-11-07", "2029-12-06", "2030-01-05", "2030-02-04", "2030-03-05", "2030-04-04",
            // 1452
            "2030-05-04", "2030-06-02", "2030-07-02", "2030-07-31", "2030-08-30", "2030-09-28",
            "2030-10-28", "2030-11-26", "2030-12-26", "2031-01-24", "2031-02-22", "2031-03-24",
            // 1453
            "2031-04-23", "2031-05-22", "2031-06-21", "2031-07-21", "2031-08-19", "2031-09-18",
            "2031-10-17", "2031-11-16", "2031-12-15", "2032-01-14", "2032-02-12", "2032-03-13",
            // 1454
            "2032-04-11", "2032-05-10", "2032-06-09", "2032-07-09", "2032-08-07", "2032-09-06",
            "2032-10-06", "2032-11-04", "2032-12-04", "2033-01-02", "2033-02-01", "2033-03-02",
    };

    private static final LocalDate[] START_DATES = new LocalDate[STARTS.length];

    static {
        for (int i = 0; i < STARTS.length; i++) START_DATES[i] = LocalDate.parse(STARTS[i]);
    }

    private static final String[] SHORT_MONTHS = {
            "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
    };

    /** The first day of a KHGT month, or null when the year is outside the table. */
    static LocalDate start(int year, int month) {
        int i = (year - FIRST_YEAR) * 12 + (month - 1);
        return i >= 0 && i < START_DATES.length ? START_DATES[i] : null;
    }

    /** "3 Jumadilakhir 1448 H", or null when the date is outside the table. */
    static String dateLabel(LocalDate date) {
        int i = indexOnOrBefore(date);
        if (i < 0) return null;
        long day = ChronoUnit.DAYS.between(START_DATES[i], date) + 1;
        if (day > 30) return null;                 // past the last tabulated month
        return day + " " + MONTHS[i % 12] + " " + (FIRST_YEAR + i / 12) + " H";
    }

    /** Last month start at or before {@code date}, or -1 before the first entry. */
    private static int indexOnOrBefore(LocalDate date) {
        int lo = 0, hi = START_DATES.length - 1, found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (START_DATES[mid].isAfter(date)) {
                hi = mid - 1;
            } else {
                found = mid;
                lo = mid + 1;
            }
        }
        return found;
    }

    /** One of the three events the panel counts down to, with the year it falls in. */
    static final class Event {
        final int year;
        final LocalDate date;
        final long days;

        Event(int year, LocalDate date, long days) {
            this.year = year;
            this.date = date;
            this.days = days;
        }
    }

    /** This year's event if it has not passed, otherwise the next year's; null past the table. */
    static Event nextEvent(LocalDate today, int month, int day) {
        int lastYear = FIRST_YEAR + START_DATES.length / 12 - 1;
        for (int year = FIRST_YEAR; year <= lastYear; year++) {
            LocalDate start = start(year, month);
            if (start == null) continue;
            LocalDate date = start.plusDays(day - 1L);
            if (!date.isBefore(today)) {
                return new Event(year, date, ChronoUnit.DAYS.between(today, date));
            }
        }
        return null;
    }

    /** "8 Feb 2027", the short Gregorian form the panel puts beside each event. */
    static String shortDate(LocalDate date) {
        return date.getDayOfMonth() + " " + SHORT_MONTHS[date.getMonthValue() - 1] + " " + date.getYear();
    }
}
