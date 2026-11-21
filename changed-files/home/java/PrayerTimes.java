package com.gathin.tvhome;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Islamic prayer times for a place, computed offline. The angles follow Kemenag (Bimas Islam): Subuh
 * at 20 degrees below the horizon and Isya at 18, with the two-minute ihtiyati (safety margin) its
 * published tables carry. No network and no location permission: the place comes from config.xml.
 *
 * Pure maths, no Android types, so it runs in JVM tests. The small per-prayer corrections below are
 * what makes the result land on the minute of Kemenag's published Bantul table (see PrayerTimesTest).
 */
final class PrayerTimes {

    /** Order used everywhere: Subuh, Dzuhur, Ashar, Maghrib, Isya. */
    static final int SUBUH = 0, DZUHUR = 1, ASHAR = 2, MAGHRIB = 3, ISYA = 4, COUNT = 5;

    /** Kemenag's Subuh and Isya twilight angles, degrees below the horizon. */
    private static final double FAJR_ANGLE = 20, ISHA_ANGLE = 18;
    /** Sunrise and sunset: the sun's centre 0.833 degrees below the horizon (refraction and semidiameter). */
    private static final double HORIZON_ANGLE = 0.833;
    /** Kemenag's ihtiyati: prayer starts are pushed back this many minutes, sunrise brought forward. */
    private static final int IHTIYATI = 2;
    /**
     * One more minute on the transit-based times (Dzuhur and Maghrib). Kemenag's own tables round
     * those up a minute further; without this the panel sits a minute before the published schedule.
     */
    private static final int TRANSIT_EXTRA = 1;

    private PrayerTimes() {
    }

    /**
     * Minutes after local midnight of Subuh, Dzuhur, Ashar, Maghrib and Isya on {@code date}.
     *
     * @param subuhAdjust extra minutes on Subuh alone: the +8 that moves the Kemenag time onto the
     *                    Muhammadiyah ruling, whose 18-degree fajr is about that much later
     */
    static int[] forDate(LocalDate date, double latitude, double longitude, ZoneId zone, int subuhAdjust) {
        int offset = zone.getRules().getOffset(date.atTime(12, 0)).getTotalSeconds() / 60;
        double[] sun = solarPosition(date, longitude, offset);
        double decl = sun[0];
        double eqTime = sun[1];
        double lat = Math.toRadians(latitude);

        // Local solar noon, the anchor every time is measured from.
        double noon = 720 - 4 * longitude - eqTime + offset;
        // The sun's altitude at Asr under the Shafi'i rule: an object's shadow as long as itself plus its own.
        double asrAltitude = Math.toDegrees(Math.atan(1 / (1 + Math.tan(Math.abs(lat - decl)))));

        double fajr = noon - 4 * hourAngle(-FAJR_ANGLE, lat, decl);
        double asr = noon + 4 * hourAngle(asrAltitude, lat, decl);
        double sunset = noon + 4 * hourAngle(-HORIZON_ANGLE, lat, decl);
        double isha = noon + 4 * hourAngle(-ISHA_ANGLE, lat, decl);

        return new int[]{
                ceilMinute(fajr + IHTIYATI) + subuhAdjust,
                ceilMinute(noon + IHTIYATI + TRANSIT_EXTRA),
                ceilMinute(asr + IHTIYATI),
                ceilMinute(sunset + IHTIYATI + TRANSIT_EXTRA),
                ceilMinute(isha + IHTIYATI)};
    }

    /**
     * NOAA's solar position for the day: {@code {declination in radians, equation of time in minutes}},
     * taken at the moment the sun crosses the place's meridian.
     */
    private static double[] solarPosition(LocalDate date, double longitude, int offsetMinutes) {
        double julianDay = julianDay(date, 12 - offsetMinutes / 60.0 + longitude / 15);
        double t = (julianDay - 2451545.0) / 36525;
        double l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)) % 360;
        double m = 357.52911 + t * (35999.05029 - 0.0001537 * t);
        double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
        double mr = Math.toRadians(m);
        double c = Math.sin(mr) * (1.914602 - t * (0.004817 + 0.000014 * t))
                + Math.sin(2 * mr) * (0.019993 - 0.000101 * t)
                + Math.sin(3 * mr) * 0.000289;
        double omega = 125.04 - 1934.136 * t;
        double lambda = l0 + c - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));
        double eps0 = 23 + (26 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60;
        double eps = eps0 + 0.00256 * Math.cos(Math.toRadians(omega));
        double decl = Math.asin(Math.sin(Math.toRadians(eps)) * Math.sin(Math.toRadians(lambda)));
        // Equation of time, the difference between apparent and mean solar time: minutes of clock.
        double y = Math.pow(Math.tan(Math.toRadians(eps) / 2), 2);
        double l0r = Math.toRadians(l0);
        double eqTime = 4 * Math.toDegrees(y * Math.sin(2 * l0r) - 2 * e * Math.sin(mr)
                + 4 * e * y * Math.sin(mr) * Math.cos(2 * l0r)
                - 0.5 * y * y * Math.sin(4 * l0r) - 1.25 * e * e * Math.sin(2 * mr));
        return new double[]{decl, eqTime};
    }

    /** Julian day at a UT hour of {@code date}, the input NOAA's series is written in. */
    private static double julianDay(LocalDate date, double utHour) {
        int year = date.getYear(), month = date.getMonthValue();
        if (month <= 2) {
            year--;
            month += 12;
        }
        int a = year / 100, b = 2 - a + a / 4;
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1))
                + date.getDayOfMonth() + b - 1524.5 + utHour / 24;
    }

    /**
     * Index of the next prayer after {@code secondOfDay}, or {@link #SUBUH} when all of today's have
     * passed and the next is tomorrow's Subuh.
     */
    static int next(int[] times, int secondOfDay) {
        for (int i = 0; i < COUNT; i++) {
            if (times[i] * 60 > secondOfDay) return i;
        }
        return SUBUH;
    }

    /** Degrees of hour angle at which the sun stands {@code altitude} degrees above the horizon. */
    private static double hourAngle(double altitude, double lat, double decl) {
        double cos = (Math.sin(Math.toRadians(altitude)) - Math.sin(lat) * Math.sin(decl))
                / (Math.cos(lat) * Math.cos(decl));
        // Rounding can push a polar day just past the cosine's range; clamp so acos stays defined.
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
    }

    /** Prays are never called early: a fraction of a minute rounds up to the next whole one. */
    private static int ceilMinute(double minuteOfDay) {
        return (int) Math.ceil(minuteOfDay - 1e-6);
    }
}
