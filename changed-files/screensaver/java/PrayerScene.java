package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The prayer screensaver's face (dream_prayer.xml): the time, the Javanese day and pasaran, the date,
 * the KHGT Hijri date, today's five prayer times with the next one in gold, and a verse of the Quran
 * with its translation (res/raw/ayat.json) that changes every few minutes.
 *
 * <p>Drawn over {@link IslamicPattern}; {@link PrayerDream} sizes it for the screen and ticks it
 * once a minute. No network and no images, so it costs the TV almost nothing while it sits there.
 */
final class PrayerScene {
    private static final String TAG = "TvHome.Dream";
    private static final long VERSE_MS = 3 * 60_000L;

    final View content;
    private final Context ctx;
    private final IslamicPattern pattern;
    private final TextView day, date, hijri, next, arabic, meaning, ref;
    /** The glass card of prayer times: one cell each, its name above its time, gold rules between. */
    private final LinearLayout prayers;
    private final LinearLayout[] cells = new LinearLayout[PrayerTimes.COUNT];
    private final TextView[] cellNames = new TextView[cells.length], cellTimes = new TextView[cells.length];
    private final View[] rules = new View[cells.length - 1];
    private final List<String[]> verses = new ArrayList<>();
    private final Random random = new Random();
    private int nextShown = -2, verseIndex;
    private long verseShownAt;

    PrayerScene(Context ctx, ViewGroup root) {
        this.ctx = ctx;
        pattern = new IslamicPattern(ctx);
        root.addView(pattern, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        content = root.findViewById(R.id.islamicContent);
        day = root.findViewById(R.id.islamicDay);
        date = root.findViewById(R.id.islamicDate);
        hijri = root.findViewById(R.id.islamicHijri);
        prayers = root.findViewById(R.id.islamicPrayers);
        Typeface outfit = ctx.getResources().getFont(R.font.outfit);
        Typeface serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD);
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                rules[i - 1] = new View(ctx);
                rules[i - 1].setBackgroundColor(0x80E9CB7E);
                prayers.addView(rules[i - 1]);
            }
            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView name = new TextView(ctx), at = new TextView(ctx);
            name.setTypeface(outfit);
            name.setIncludeFontPadding(false);
            name.setText(PrayerDay.name(ctx, i));
            name.setGravity(Gravity.CENTER_HORIZONTAL);
            at.setGravity(Gravity.CENTER_HORIZONTAL);
            at.setTypeface(serifBold);
            at.setIncludeFontPadding(false);
            cell.addView(name);
            cell.addView(at);
            prayers.addView(cell);
            cells[i] = cell;
            cellNames[i] = name;
            cellTimes[i] = at;
        }
        next = root.findViewById(R.id.islamicNext);
        arabic = root.findViewById(R.id.islamicArabic);
        meaning = root.findViewById(R.id.islamicMeaning);
        ref = root.findViewById(R.id.islamicRef);
        loadVerses();
        verseIndex = verses.isEmpty() ? 0 : random.nextInt(verses.size());
        showVerse();
        update();
    }

    /** Lays the face out for a screen {@code w} by {@code h} pixels, in units of a screen 380 tall. */
    void size(int w, int h) {
        float u = h / 380f;
        TextView time = content.findViewById(R.id.islamicTime);
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, 70f * u);
        // The clock in gold, lighter at the top of the digits, with a soft glow around it.
        float fs = time.getTextSize();
        time.getPaint().setShader(new android.graphics.LinearGradient(0, 0, 0, fs * 0.8f,
                new int[]{0xFFFBE7B0, 0xFFE9C676, 0xFFC4943F}, new float[]{0f, 0.5f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        time.setShadowLayer(14f * u, 0, 0, 0x55E9CB7E);
        // Room for the glow inside the view, or it is cut off square at the view's edges.
        int glow = Math.round(16f * u);
        time.setPadding(glow, glow, glow, glow);
        time.invalidate();
        px(R.id.islamicDay, 8.5f * u);
        px(R.id.islamicDate, 12.5f * u);
        px(R.id.islamicHijri, 12.5f * u);
        // The gold rule between the time and the dates, as tall as the digits.
        View rule = content.findViewById(R.id.islamicRule);
        LinearLayout.LayoutParams rl = (LinearLayout.LayoutParams) rule.getLayoutParams();
        rl.width = Math.max(1, Math.round(0.8f * u));
        rl.height = Math.round(44f * u);
        rl.leftMargin = 0;
        rl.rightMargin = Math.round(16f * u);
        px(R.id.islamicOrnament, 8f * u);
        int cellW = Math.round(62f * u), pad = Math.round(4f * u);
        prayers.setPadding(pad, pad, pad, pad);
        for (int i = 0; i < cells.length; i++) {
            cellNames[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, 8.5f * u);
            cellTimes[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * u);
            ((LinearLayout.LayoutParams) cellTimes[i].getLayoutParams()).topMargin = Math.round(4f * u);
            cells[i].setPadding(0, Math.round(5f * u), 0, Math.round(6f * u));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) cells[i].getLayoutParams();
            lp.width = cellW;
            lp.leftMargin = lp.rightMargin = Math.round(3f * u);
            if (i > 0) {
                LinearLayout.LayoutParams rp = (LinearLayout.LayoutParams) rules[i - 1].getLayoutParams();
                rp.width = Math.max(1, Math.round(0.6f * u));
                rp.height = Math.round(24f * u);
            }
        }
        px(R.id.islamicNext, 10f * u);
        arabic.setTextSize(TypedValue.COMPLEX_UNIT_PX, 12.5f * u);
        meaning.setTextSize(TypedValue.COMPLEX_UNIT_PX, 7.5f * u);
        ref.setTextSize(TypedValue.COMPLEX_UNIT_PX, 6.5f * u);
        int wide = Math.round(w * 0.55f);
        arabic.setMaxWidth(wide);
        meaning.setMaxWidth(wide);
        top(R.id.islamicDate, 6f * u);
        top(R.id.islamicHijri, 5f * u);
        top(R.id.islamicOrnament, 16f * u);
        top(R.id.islamicPrayers, 13f * u);
        top(R.id.islamicNext, 7f * u);
        top(R.id.islamicVerse, 14f * u);
        top(R.id.islamicMeaning, 6f * u);
        top(R.id.islamicRef, 7f * u);
        content.requestLayout();
    }

    /** Once a minute: the dates and prayer times, the lattice drifted a little, a new verse when due. */
    void minute() {
        update();
        pattern.drift(random.nextFloat() * 6f - 3f, random.nextFloat() * 6f - 3f);
        if (android.os.SystemClock.elapsedRealtime() - verseShownAt >= VERSE_MS && verses.size() > 1) {
            verseIndex = (verseIndex + 1) % verses.size();
            showVerse();
        }
    }

    private void update() {
        LocalDate today = LocalDate.now();
        Locale locale = ctx.getResources().getConfiguration().getLocales().get(0);
        day.setText(Weton.today(ctx.getResources()));
        date.setText(today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)));
        String label = Hijri.dateLabel(today);
        hijri.setText(label != null ? label : "");
        hijri.setVisibility(label != null ? View.VISIBLE : View.GONE);

        int[] minutes = PrayerDay.times(ctx, today);
        java.time.LocalTime now = java.time.LocalTime.now();
        int nowMin = now.getHour() * 60 + now.getMinute();
        // After Isya, tomorrow's Subuh is the one framed (the same rule Home uses).
        boolean tomorrow = nowMin >= minutes[PrayerTimes.ISYA];
        int n = tomorrow ? PrayerTimes.SUBUH : PrayerTimes.next(minutes, now.toSecondOfDay());
        int framed = n;
        for (int i = 0; i < cells.length; i++) {
            cellTimes[i].setText(PrayerDay.clock(minutes[i]));
            if (framed == nextShown) continue;
            boolean on = i == framed;
            cells[i].setBackgroundResource(on ? R.drawable.prayer_cell_next : 0);
            cellNames[i].setTextColor(on ? 0xFFE9CB7E : 0xB3F4EEDC);
            cellTimes[i].setTextColor(on ? 0xFFF2CF7A : 0xFFF4EEDC);
            cellTimes[i].setShadowLayer(on ? 8f : 0f, 0, 0, 0x66E9CB7E);
        }
        nextShown = framed;
        // The rules either side of the framed cell give way to its frame.
        for (int i = 0; i < rules.length; i++) {
            rules[i].setVisibility(i == framed - 1 || i == framed ? View.INVISIBLE : View.VISIBLE);
        }
        prayers.setVisibility(View.VISIBLE);

        int left;
        if (!tomorrow) {
            left = minutes[n] - nowMin;
        } else {
            int[] next_ = PrayerDay.times(ctx, today.plusDays(1));
            left = 24 * 60 - nowMin + next_[PrayerTimes.SUBUH];
        }
        String name = PrayerDay.name(ctx, n);
        if (left >= 60) {
            next.setText(ctx.getString(R.string.dream_next_hm, name, left / 60, left % 60));
        } else if (left >= 1) {
            next.setText(ctx.getString(R.string.dream_next_m, name, left));
        } else {
            next.setText(ctx.getString(R.string.dream_next_now, name));
        }
        next.setVisibility(View.VISIBLE);
    }

    private void showVerse() {
        verseShownAt = android.os.SystemClock.elapsedRealtime();
        if (verses.isEmpty()) {
            content.findViewById(R.id.islamicVerse).setVisibility(View.GONE);
            return;
        }
        String[] v = verses.get(verseIndex);
        arabic.setText(v[0]);
        meaning.setText(v[1]);
        ref.setText(v[2]);
        content.findViewById(R.id.islamicVerse).setVisibility(View.VISIBLE);
    }

    /** Frees the pattern bitmap; the screensaver is going away. */
    void release() {
        pattern.setVisibility(View.GONE);
    }

    private void loadVerses() {
        try (InputStream in = ctx.getResources().openRawResource(R.raw.ayat)) {
            byte[] buf = new byte[64 * 1024];
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
            JSONArray a = new JSONObject(new String(buf, 0, n, StandardCharsets.UTF_8)).getJSONArray("ayat");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                verses.add(new String[]{o.getString("ar"), o.getString("tr"), o.getString("ref")});
            }
        } catch (Exception e) {
            Log.w(TAG, "no verses: " + e.getMessage());
        }
    }

    private void px(int id, float size) {
        ((TextView) content.findViewById(id)).setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    private void top(int id, float margin) {
        View v = content.findViewById(id);
        ((LinearLayout.LayoutParams) v.getLayoutParams()).topMargin = Math.round(margin);
    }
}
