package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Open-Meteo's forecast and air-quality APIs for the Home weather block: no key, no account. The two
 * responses are kept as their raw bodies so they can be cached and parsed again after a restart.
 */
final class Weather {

    /** Today plus the three forecast days. */
    private static final int DAYS = 4;
    private static final int TIMEOUT_MS = 10_000;
    private static final String TIME_ZONE = "Asia%2FJakarta";

    private Weather() {
    }

    /** One daily entry: the date, its WMO code and the day's high and low in °C. */
    static final class Day {
        final LocalDate date;
        final int code;
        final double max, min;

        Day(LocalDate date, int code, double max, double min) {
            this.date = date;
            this.code = code;
            this.max = max;
            this.min = min;
        }
    }

    /** What the block shows: now, the air and the days. */
    static final class Snapshot {
        final double temperature;
        final int code;
        /** US AQI, or -1 when the air-quality call failed. */
        final int aqi;
        /** PM2.5 in µg/m³, or -1 when it failed. */
        final double pm25;
        /** Today first, then the forecast days. */
        final List<Day> days;

        Snapshot(double temperature, int code, int aqi, double pm25, List<Day> days) {
            this.temperature = temperature;
            this.code = code;
            this.aqi = aqi;
            this.pm25 = pm25;
            this.days = days;
        }
    }

    /** The forecast body and the air body, the second null when only the air call failed. */
    static String[] fetch(double latitude, double longitude) throws IOException {
        String forecast = get(forecastUrl(latitude, longitude));
        String air = null;
        try {
            air = get(airUrl(latitude, longitude));
        } catch (IOException ignored) {
            // The forecast alone still fills the block; the air line stays hidden.
        }
        return new String[]{forecast, air};
    }

    static Snapshot parse(String forecastJson, String airJson) throws JSONException {
        JSONObject forecast = new JSONObject(forecastJson);
        JSONObject current = forecast.getJSONObject("current");
        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray times = daily.getJSONArray("time");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray highs = daily.getJSONArray("temperature_2m_max");
        JSONArray lows = daily.getJSONArray("temperature_2m_min");
        List<Day> days = new ArrayList<>();
        for (int i = 0; i < times.length() && i < DAYS; i++) {
            days.add(new Day(LocalDate.parse(times.getString(i)), codes.getInt(i),
                    highs.getDouble(i), lows.getDouble(i)));
        }

        int aqi = -1;
        double pm25 = -1;
        if (airJson != null) {
            try {
                JSONObject air = new JSONObject(airJson).getJSONObject("current");
                aqi = Math.round((float) air.getDouble("us_aqi"));
                pm25 = air.getDouble("pm2_5");
            } catch (JSONException ignored) {
                // Leave both at -1: the line is dropped rather than shown with a wrong value.
            }
        }
        return new Snapshot(current.getDouble("temperature_2m"), current.getInt("weather_code"),
                aqi, pm25, days);
    }

    /** The condition as a resource id, from Open-Meteo's WMO weather code. */
    static int describe(int code) {
        switch (code) {
            case 0:
                return R.string.weather_clear;
            case 1:
                return R.string.weather_mainly_clear;
            case 2:
                return R.string.weather_partly_cloudy;
            case 3:
                return R.string.weather_overcast;
            case 45:
            case 48:
                return R.string.weather_fog;
            case 51:
            case 53:
            case 55:
            case 56:
            case 57:
                return R.string.weather_drizzle;
            case 61:
            case 63:
            case 65:
            case 66:
            case 67:
                return R.string.weather_rain;
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86:
                return R.string.weather_snow;
            case 80:
            case 81:
            case 82:
                return R.string.weather_showers;
            case 95:
            case 96:
            case 99:
                return R.string.weather_thunderstorm;
            default:
                return R.string.weather_unknown;
        }
    }

    /** The US AQI category as a resource id, with Open-Meteo's us_aqi. */
    static int aqiCategory(int aqi) {
        if (aqi <= 50) return R.string.aqi_good;
        if (aqi <= 100) return R.string.aqi_moderate;
        if (aqi <= 150) return R.string.aqi_unhealthy_sensitive;
        if (aqi <= 200) return R.string.aqi_unhealthy;
        if (aqi <= 300) return R.string.aqi_very_unhealthy;
        return R.string.aqi_hazardous;
    }

    private static String forecastUrl(double latitude, double longitude) {
        return "https://api.open-meteo.com/v1/forecast?latitude=" + coordinate(latitude)
                + "&longitude=" + coordinate(longitude)
                + "&current=temperature_2m,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&timezone=" + TIME_ZONE + "&forecast_days=" + DAYS;
    }

    private static String airUrl(double latitude, double longitude) {
        return "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + coordinate(latitude)
                + "&longitude=" + coordinate(longitude)
                + "&current=us_aqi,pm2_5&timezone=" + TIME_ZONE;
    }

    /** A decimal degree with a dot, whatever the device locale uses for decimals. */
    private static String coordinate(double value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private static String get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "CleanYourTV");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + status);
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }
}
