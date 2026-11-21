package com.gathin.tvhome;

import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;

/**
 * Screensaver: the time, the Javanese and KHGT Hijri dates and today's prayer times over a lattice
 * of eight-pointed stars (PrayerScene), with the next prayer framed in gold and a Quran verse
 * underneath. Pick it in Settings, or over adb:
 * {@code settings put secure screensaver_components com.gathin.tvhome/.PrayerDream}.
 *
 * <p>Separate from {@link ClockDream} on purpose: Home's own screensaver and the plain clock stay
 * exactly as they were, and this one can be chosen only when wanted.
 */
public class PrayerDream extends DreamService {

    /** The countdown and the dates only need a fresh minute; 20 s keeps the minute honest. */
    private static final long TICK_MS = 20_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private PrayerScene scene;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (scene != null) {
                scene.minute();
                nudge();
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // the theme's locale, as ClockDream
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        // The prayer times and the azan are the point of this dream, so keep the panel legible.
        setScreenBright(true);
        setContentView(R.layout.dream_prayer);
        ViewGroup root = findViewById(R.id.islamicRoot);
        scene = new PrayerScene(this, root);
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol || b - t != ob - ot) {
                scene.size(r - l, b - t);
                nudge();
            }
        });
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (scene == null) return;
        scene.content.setAlpha(0f);
        scene.content.post(() -> scene.content.animate().alpha(1f).setDuration(1200).start());
        handler.post(tick);
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(tick);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        if (scene != null) scene.release();
        super.onDetachedFromWindow();
    }

    /** Shifts the face up to 1 % of the screen either way, so nothing burns in. */
    private void nudge() {
        if (scene == null) return;
        View parent = (View) scene.content.getParent();
        if (parent.getWidth() == 0 || parent.getHeight() == 0) return;
        scene.content.setTranslationX(parent.getWidth() * 0.01f * (2f * random.nextFloat() - 1f));
        scene.content.setTranslationY(parent.getHeight() * 0.01f * (2f * random.nextFloat() - 1f));
    }
}
