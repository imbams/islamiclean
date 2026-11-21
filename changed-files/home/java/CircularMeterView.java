package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Keep;

/**
 * Circular progress meter widget (60x60 dp) displaying a track ring and colored progress arc.
 */
@Keep
public class CircularMeterView extends View {
    private int progress = 0;
    private int max = 100;
    private int meterColor = 0xFFFFD8A6;
    private final int trackColor = 0x26FFFFFF;
    private final int circleBgColor = 0x1A000000;
    private float strokeWidthPx;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    public CircularMeterView(Context context) {
        super(context);
        init();
    }

    public CircularMeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularMeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokeWidthPx = getResources().getDisplayMetrics().density * 3.5f;

        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(circleBgColor);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidthPx);
        trackPaint.setColor(trackColor);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidthPx);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(meterColor);
    }

    public void setMeterColor(int color) {
        this.meterColor = color;
        progressPaint.setColor(color);
        invalidate();
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(max, progress));
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    public void setMax(int max) {
        this.max = max > 0 ? max : 100;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = strokeWidthPx / 2f + 1f;
        arcRect.set(pad, pad, w - pad, h - pad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawOval(arcRect, bgPaint);
        canvas.drawOval(arcRect, trackPaint);
        if (progress > 0) {
            float sweep = (progress / (float) max) * 360f;
            canvas.drawArc(arcRect, -90f, sweep, false, progressPaint);
        }
    }
}
