/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-03-20
 */
package com.gasleak.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import com.gasleak.data.model.HourlyStatPoint;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class StatsChartView extends View {

    private static final float PAD_LEFT   = 60f;
    private static final float PAD_RIGHT  = 16f;
    private static final float PAD_TOP    = 20f;
    private static final float PAD_BOT    = 52f;
    private static final float DOT_R      = 5f;
    private static final float STROKE_W   = 2.5f;
    private static final int   GRID_COUNT = 4;

    private final Paint linePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<HourlyStatPoint> points = new ArrayList<>();

    public StatsChartView(Context context) { super(context); init(); }
    public StatsChartView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        linePaint.setColor(0xFF4CAF50);
        linePaint.setStrokeWidth(STROKE_W);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint.setColor(0xFF4CAF50);
        dotPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(0x22FFFFFF);
        gridPaint.setStrokeWidth(1f);

        labelPaint.setColor(0xAAFFFFFF);
        labelPaint.setTextSize(26f);
    }

    public void setPoints(List<HourlyStatPoint> pts) {
        points = (pts != null) ? pts : new ArrayList<HourlyStatPoint>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFF0D0D0D);
        if (points.isEmpty()) return;

        float w = getWidth(), h = getHeight();
        float cl = PAD_LEFT, cr = w - PAD_RIGHT;
        float ct = PAD_TOP,  cb = h - PAD_BOT;
        float cw = cr - cl,  ch = cb - ct;

        float maxVal = 0;
        for (HourlyStatPoint p : points) if (p.getAvgGas() > maxVal) maxVal = p.getAvgGas();
        float topVal = maxVal > 0 ? maxVal * 1.2f : 1f;

        drawGrid(canvas, cl, ct, cr, cb, ch, topVal);

        int n = points.size();
        float step = n > 1 ? cw / (n - 1) : 0;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = cl + i * step;
            ys[i] = cb - (points.get(i).getAvgGas() / topVal) * ch;
        }

        drawFill(canvas, xs, ys, cb);
        drawLine(canvas, xs, ys);
        drawDots(canvas, xs, ys);
        drawLabels(canvas, xs, cb);
    }

    private void drawGrid(Canvas canvas, float cl, float ct, float cr, float cb, float ch, float topVal) {
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= GRID_COUNT; i++) {
            float y = cb - ch * i / GRID_COUNT;
            canvas.drawLine(cl, y, cr, y, gridPaint);
            canvas.drawText(String.valueOf((int)(topVal * i / GRID_COUNT)), cl - 6f, y + 9f, labelPaint);
        }
    }

    private void drawFill(Canvas canvas, float[] xs, float[] ys, float bottom) {
        Path path = new Path();
        path.moveTo(xs[0], bottom);
        path.lineTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) path.lineTo(xs[i], ys[i]);
        path.lineTo(xs[xs.length - 1], bottom);
        path.close();
        fillPaint.setShader(new LinearGradient(0, PAD_TOP, 0, bottom,
            0x554CAF50, 0x004CAF50, Shader.TileMode.CLAMP));
        canvas.drawPath(path, fillPaint);
    }

    private void drawLine(Canvas canvas, float[] xs, float[] ys) {
        Path path = new Path();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) path.lineTo(xs[i], ys[i]);
        canvas.drawPath(path, linePaint);
    }

    private void drawDots(Canvas canvas, float[] xs, float[] ys) {
        for (int i = 0; i < xs.length; i++) canvas.drawCircle(xs[i], ys[i], DOT_R, dotPaint);
    }

    private void drawLabels(Canvas canvas, float[] xs, float cb) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        labelPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < points.size(); i++) {
            String label = parseBucketLabel(points.get(i).getBucket(), sdf);
            canvas.save();
            canvas.rotate(-45f, xs[i], cb + 14f);
            canvas.drawText(label, xs[i], cb + 14f, labelPaint);
            canvas.restore();
        }
    }

    private String parseBucketLabel(String bucket, SimpleDateFormat sdf) {
        if (bucket == null || bucket.isEmpty()) return "--";
        try {
            String norm = bucket;
            if (norm.endsWith("Z")) norm = norm.substring(0, norm.length() - 1) + "+0000";
            if (norm.length() > 6) {
                String tail = norm.substring(norm.length() - 6);
                if (tail.matches("[+-]\\d{2}:\\d{2}"))
                    norm = norm.substring(0, norm.length() - 6) + tail.replace(":", "");
            }
            SimpleDateFormat p = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
            p.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = p.parse(norm);
            return d != null ? sdf.format(d) : bucket.substring(11, Math.min(16, bucket.length()));
        } catch (Exception e) {
            return bucket.length() >= 16 ? bucket.substring(11, 16) : bucket;
        }
    }
}
