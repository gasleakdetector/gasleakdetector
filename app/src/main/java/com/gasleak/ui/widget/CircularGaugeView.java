/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-02-20
 */
package com.gasleak.ui.widget;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import com.gasleak.data.model.GasStatus;

/**
 * Circular arc gauge that displays a gas concentration value (0–1000 ppm).
 * Color interpolates from green → amber → red as the value crosses the
 * warning and danger thresholds defined in {@link GasStatus}.
 */
public class CircularGaugeView extends View {

    private static final float STROKE_WIDTH      = 24f;
    private static final float START_ANGLE       = 135f;
    private static final float SWEEP_ANGLE       = 270f;
    private static final int   MAX_VALUE         = 1000;
    private static final int   ANIMATION_DURATION = 800;

    /* Gauge arc colors — kept as constants because they drive custom blending logic,
     * not simple theme colors. */
    private static final int COLOR_NORMAL  = 0xFF4CAF50;
    private static final int COLOR_WARNING = 0xFFFFC107;
    private static final int COLOR_DANGER  = 0xFFF44336;
    private static final int COLOR_TRACK   = 0xFF424242; // background arc

    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF arcRect;

    private float animatedValue = 0f;
    private int   animatedColor = COLOR_NORMAL;

    private ValueAnimator valueAnimator;
    private ValueAnimator colorAnimator;

    public CircularGaugeView(Context context) {
        super(context);
        init();
    }

    public CircularGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(STROKE_WIDTH);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        backgroundPaint.setColor(COLOR_TRACK);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(STROKE_WIDTH);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(COLOR_NORMAL);

        arcRect = new RectF();
    }

    /** Animates the gauge to the new value. Clamps to [0, MAX_VALUE]. */
    public void setValue(int value) {
        int clamped = Math.max(0, Math.min(value, MAX_VALUE));
        animateToValue(clamped);
    }

    /** Sets the gauge instantly without animation — useful for initial state restoration. */
    public void setValueImmediate(int value) {
        int clamped = Math.max(0, Math.min(value, MAX_VALUE));
        animatedValue = clamped;
        animatedColor = getColorForValue(clamped);
        progressPaint.setColor(animatedColor);
        invalidate();
    }

    private void animateToValue(int target) {
        if (valueAnimator != null) valueAnimator.cancel();
        if (colorAnimator != null) colorAnimator.cancel();

        valueAnimator = ValueAnimator.ofFloat(animatedValue, target);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animatedValue = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        valueAnimator.start();

        colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), animatedColor, getColorForValue(target));
        colorAnimator.setDuration(ANIMATION_DURATION);
        colorAnimator.setInterpolator(new DecelerateInterpolator());
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animatedColor = (int) animation.getAnimatedValue();
                progressPaint.setColor(animatedColor);
            }
        });
        colorAnimator.start();
    }

    /**
     * Blends between the three status colors based on ppm value.
     * Below warning → solid green. Warning to danger → green→amber blend.
     * Above danger → amber→red blend.
     */
    private int getColorForValue(int value) {
        if (value < GasStatus.WARNING_THRESHOLD) {
            return COLOR_NORMAL;
        } else if (value < GasStatus.DANGER_THRESHOLD) {
            float ratio = (value - GasStatus.WARNING_THRESHOLD)
                        / (float) (GasStatus.DANGER_THRESHOLD - GasStatus.WARNING_THRESHOLD);
            return blendColors(COLOR_NORMAL, COLOR_WARNING, ratio);
        } else {
            float ratio = Math.min(1f, (value - GasStatus.DANGER_THRESHOLD) / 200f);
            return blendColors(COLOR_WARNING, COLOR_DANGER, ratio);
        }
    }

    private int blendColors(int colorStart, int colorEnd, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        int r = (int) (((colorStart >> 16) & 0xFF) + (((colorEnd >> 16) & 0xFF) - ((colorStart >> 16) & 0xFF)) * ratio);
        int g = (int) (((colorStart >>  8) & 0xFF) + (((colorEnd >>  8) & 0xFF) - ((colorStart >>  8) & 0xFF)) * ratio);
        int b = (int) (((colorStart)       & 0xFF) + (((colorEnd)       & 0xFF) - ((colorStart)       & 0xFF)) * ratio);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = STROKE_WIDTH / 2;
        arcRect.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(arcRect, START_ANGLE, SWEEP_ANGLE, false, backgroundPaint);
        canvas.drawArc(arcRect, START_ANGLE, SWEEP_ANGLE * (animatedValue / MAX_VALUE), false, progressPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (valueAnimator != null) valueAnimator.cancel();
        if (colorAnimator != null) colorAnimator.cancel();
    }
}
