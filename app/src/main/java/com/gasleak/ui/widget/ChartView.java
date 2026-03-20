/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-03-15
 */
package com.gasleak.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;
import com.gasleak.data.model.HistoricalDataPoint;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Scrollable, zoomable line chart for gas ppm readings.
 *
 * Supports:
 *  - Pinch-to-zoom (adjusts node spacing)
 *  - Fling scrolling
 *  - Single-tap to select/deselect a node, with crosshair overlay
 */
public class ChartView extends View {

    private static final float NODE_RADIUS    = 8f;
    private static final float TOUCH_RADIUS   = 40f;
    private static final float MIN_NODE_WIDTH =  4f;
    private static final float MAX_NODE_WIDTH = 60f;
    private static final float DEFAULT_NODE_WIDTH = 10f;

    private float nodeWidth = DEFAULT_NODE_WIDTH;

    private Paint areaPaint;
    private Paint linePaint;
    private Paint nodePaint;
    private Paint selectedNodePaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint axisPaint;
    private Paint axisTextPaint;
    private Paint nodeCountPaint;
    private Path  areaPath;

    /* Cached in init() so onDraw() can use it without a Context call. */
    private String emptyMessage;

    private final List<DataPoint> dataPoints = new ArrayList<>();
    private int selectedIndex = -1;
    private OnNodeSelectedListener listener;

    private float chartLeft;
    private float chartTop;
    private float chartRight;
    private float chartBottom;

    private float   scrollX    = 0f;
    private float   maxScrollX = 0f;
    private Scroller scroller;
    private GestureDetector      gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private SimpleDateFormat timeFormat;

    public ChartView(Context context) {
        super(context);
        init(context);
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        areaPaint.setStyle(Paint.Style.FILL);
        areaPaint.setColor(0x504CAF50);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);
        linePaint.setColor(0xFF4CAF50);

        nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setStyle(Paint.Style.FILL);
        nodePaint.setColor(0xFF4CAF50);

        selectedNodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedNodePaint.setStyle(Paint.Style.FILL);
        selectedNodePaint.setColor(0xFFFFFFFF);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(0x30FFFFFF);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(24f);
        textPaint.setColor(0x80FFFFFF);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(2f);
        axisPaint.setColor(0xFFFFFFFF);

        axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisTextPaint.setTextSize(28f);
        axisTextPaint.setColor(0xFFFFFFFF);
        axisTextPaint.setTextAlign(Paint.Align.CENTER);

        nodeCountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodeCountPaint.setTextSize(26f);
        nodeCountPaint.setColor(0xFFFFFFFF);
        nodeCountPaint.setTextAlign(Paint.Align.LEFT);

        areaPath     = new Path();
        scroller     = new Scroller(context);
        timeFormat   = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        /* Cache so we don't call getString() inside onDraw(). */
        emptyMessage = "Waiting for data\u2026";

        gestureDetector      = new GestureDetector(context, new ChartGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new ChartScaleListener());
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void addDataPoint(int value) {
        addDataPointWithTimestamp(value, System.currentTimeMillis());
    }

    public void addDataPointWithTimestamp(int value, long timestamp) {
        dataPoints.add(new DataPoint(value, timestamp));
        updateScrollBounds();
        scrollToEnd();
        invalidate();
    }

    public void setDataPoints(List<HistoricalDataPoint> points) {
        dataPoints.clear();
        selectedIndex = -1;
        for (HistoricalDataPoint p : points) {
            dataPoints.add(new DataPoint(p.getGasPpm(), p.getTimestamp()));
        }
        updateScrollBounds();
        scrollToEnd();
        invalidate();
    }

    public void clearData() {
        dataPoints.clear();
        selectedIndex = -1;
        scrollX    = 0f;
        maxScrollX = 0f;
        invalidate();
    }

    public void clearSelection() {
        selectedIndex = -1;
        invalidate();
    }

    public void setOnNodeSelectedListener(OnNodeSelectedListener listener) {
        this.listener = listener;
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 60f;
        chartLeft   = padding;
        chartTop    = padding;
        chartRight  = w - padding;
        chartBottom = h - padding - 40f;
        updateScrollBounds();
        if (!dataPoints.isEmpty()) scrollToEnd();
    }

    private float getEffectiveNodeWidth() {
        if (dataPoints.size() <= 1) return nodeWidth;
        float chartWidth        = chartRight - chartLeft;
        float totalWidthNeeded  = (dataPoints.size() - 1) * nodeWidth;
        /* Expand spacing when there are few enough nodes to fill the chart width. */
        if (dataPoints.size() >= 40 && totalWidthNeeded < chartWidth) {
            return chartWidth / Math.max(1, dataPoints.size() - 1);
        }
        return nodeWidth;
    }

    private void updateScrollBounds() {
        float totalWidth   = (dataPoints.size() - 1) * getEffectiveNodeWidth();
        float visibleWidth = chartRight - chartLeft;
        maxScrollX = Math.max(0, totalWidth - visibleWidth);
    }

    private void scrollToEnd() {
        scrollX = maxScrollX;
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints.isEmpty()) {
            drawEmptyMessage(canvas);
            return;
        }
        drawAxes(canvas);
        drawGrid(canvas);
        drawChart(canvas);
        drawNodes(canvas);
        drawYAxisLabels(canvas);
        drawNodeCounter(canvas);
        if (selectedIndex >= 0) drawSelectedNodeAxis(canvas);
    }

    private void drawEmptyMessage(Canvas canvas) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(32f);
        p.setColor(0x80FFFFFF);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(emptyMessage, getWidth() / 2f, getHeight() / 2f, p);
    }

    private void drawNodeCounter(Canvas canvas) {
        String text = String.format(Locale.getDefault(), "Nodes: %d", dataPoints.size());
        float  x    = getWidth() - nodeCountPaint.measureText(text) - 10f;
        float  y    = 10f + nodeCountPaint.getTextSize() * 0.75f + 8f;
        canvas.drawText(text, x, y, nodeCountPaint);
    }

    private void drawAxes(Canvas canvas) {
        canvas.drawLine(chartLeft,  chartBottom, chartRight, chartBottom, axisPaint);
        canvas.drawLine(chartLeft,  chartTop,    chartLeft,  chartBottom, axisPaint);
        canvas.drawLine(chartRight, chartTop,    chartRight, chartBottom, axisPaint);
    }

    private void drawGrid(Canvas canvas) {
        int lines = 4;
        for (int i = 0; i <= lines; i++) {
            float y = chartTop + (chartBottom - chartTop) * i / lines;
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
        }
    }

    private void drawYAxisLabels(Canvas canvas) {
        int minValue = getMinValue();
        int maxValue = getMaxValue();
        int lines    = 4;
        textPaint.setTextAlign(Paint.Align.RIGHT);
        float textHalfHeight = (textPaint.descent() - textPaint.ascent()) / 2f - textPaint.descent();
        for (int i = 0; i <= lines; i++) {
            float ratio = (float) i / lines;
            int   value = (int) (maxValue - ratio * (maxValue - minValue));
            float y     = chartTop + (chartBottom - chartTop) * ratio;
            canvas.drawText(String.valueOf(value), chartLeft - 10, y + textHalfHeight, textPaint);
        }
    }

    private void drawChart(Canvas canvas) {
        areaPath.reset();
        int   minValue    = getMinValue();
        int   maxValue    = getMaxValue();
        float valueRange  = Math.max(1, maxValue - minValue);
        float ew          = getEffectiveNodeWidth();
        int   startIndex  = Math.max(0, (int) (scrollX / ew));
        int   endIndex    = Math.min(dataPoints.size(), startIndex + (int) ((chartRight - chartLeft) / ew) + 2);

        boolean first = true;
        for (int i = startIndex; i < endIndex; i++) {
            float x    = chartLeft + i * ew - scrollX;
            float norm = (dataPoints.get(i).value - minValue) / valueRange;
            float y    = chartBottom - (chartBottom - chartTop) * norm;
            if (first) { areaPath.moveTo(x, chartBottom); areaPath.lineTo(x, y); first = false; }
            else        { areaPath.lineTo(x, y); }
        }
        if (!first) {
            float lastX = chartLeft + (endIndex - 1) * ew - scrollX;
            areaPath.lineTo(lastX, chartBottom);
            areaPath.close();
            canvas.drawPath(areaPath, areaPaint);
        }

        Path linePath = new Path();
        first = true;
        for (int i = startIndex; i < endIndex; i++) {
            float x    = chartLeft + i * ew - scrollX;
            float norm = (dataPoints.get(i).value - minValue) / valueRange;
            float y    = chartBottom - (chartBottom - chartTop) * norm;
            if (first) { linePath.moveTo(x, y); first = false; }
            else        { linePath.lineTo(x, y); }
        }
        canvas.drawPath(linePath, linePaint);
    }

    private void drawNodes(Canvas canvas) {
        int   minValue   = getMinValue();
        int   maxValue   = getMaxValue();
        float valueRange = Math.max(1, maxValue - minValue);
        float ew         = getEffectiveNodeWidth();
        int   startIndex = Math.max(0, (int) (scrollX / ew));
        int   endIndex   = Math.min(dataPoints.size(), startIndex + (int) ((chartRight - chartLeft) / ew) + 2);

        for (int i = startIndex; i < endIndex; i++) {
            float x    = chartLeft + i * ew - scrollX;
            float norm = (dataPoints.get(i).value - minValue) / valueRange;
            float y    = chartBottom - (chartBottom - chartTop) * norm;
            canvas.drawCircle(x, y, NODE_RADIUS, i == selectedIndex ? selectedNodePaint : nodePaint);
        }
    }

    private void drawSelectedNodeAxis(Canvas canvas) {
        if (selectedIndex < 0 || selectedIndex >= dataPoints.size()) return;
        DataPoint selected   = dataPoints.get(selectedIndex);
        int   minValue       = getMinValue();
        int   maxValue       = getMaxValue();
        float valueRange     = Math.max(1, maxValue - minValue);
        float ew             = getEffectiveNodeWidth();
        float nodeX          = chartLeft + selectedIndex * ew - scrollX;
        float norm           = (selected.value - minValue) / valueRange;
        float nodeY          = chartBottom - (chartBottom - chartTop) * norm;

        if (nodeX >= chartLeft && nodeX <= chartRight) {
            canvas.drawLine(nodeX, chartTop, nodeX, chartBottom, axisPaint);
            canvas.drawText(timeFormat.format(new Date(selected.timestamp)), nodeX, chartBottom + 35, axisTextPaint);
        }

        canvas.drawLine(chartLeft, nodeY, chartRight, nodeY, axisPaint);
        axisTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(selected.value + " ppm", 10, nodeY - 10, axisTextPaint);
        axisTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    // ── Touch handling ────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        if (!scaleGestureDetector.isInProgress()) gestureDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP && !scroller.isFinished()) scroller.abortAnimation();
        return true;
    }

    private boolean handleTap(float touchX, float touchY) {
        int   minValue     = getMinValue();
        int   maxValue     = getMaxValue();
        float valueRange   = Math.max(1, maxValue - minValue);
        float ew           = getEffectiveNodeWidth();
        int   nearestIndex = -1;
        float nearestDist  = Float.MAX_VALUE;

        int startIndex = Math.max(0, (int) (scrollX / ew) - 1);
        int endIndex   = Math.min(dataPoints.size(), startIndex + (int) ((chartRight - chartLeft) / ew) + 3);

        for (int i = startIndex; i < endIndex; i++) {
            float x    = chartLeft + i * ew - scrollX;
            float norm = (dataPoints.get(i).value - minValue) / valueRange;
            float y    = chartBottom - (chartBottom - chartTop) * norm;
            float dist = (float) Math.sqrt(Math.pow(touchX - x, 2) + Math.pow(touchY - y, 2));
            if (dist < TOUCH_RADIUS && dist < nearestDist) { nearestDist = dist; nearestIndex = i; }
        }

        if (nearestIndex != -1) {
            selectedIndex = nearestIndex;
            invalidate();
            if (listener != null) {
                DataPoint p = dataPoints.get(nearestIndex);
                listener.onNodeSelected(nearestIndex, p.value, p.timestamp);
            }
            return true;
        }

        if (selectedIndex != -1) {
            selectedIndex = -1;
            invalidate();
            if (listener != null) listener.onNodeDeselected();
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollX = Math.max(0, Math.min(scroller.getCurrX(), maxScrollX));
            invalidate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getMinValue() {
        if (dataPoints.isEmpty()) return 0;
        int min = Integer.MAX_VALUE;
        for (DataPoint p : dataPoints) if (p.value < min) min = p.value;
        return Math.max(0, (int) (min * 0.85f));
    }

    private int getMaxValue() {
        if (dataPoints.isEmpty()) return 1000;
        int max = Integer.MIN_VALUE;
        for (DataPoint p : dataPoints) if (p.value > max) max = p.value;
        int range = max - getMinValue();
        return max + Math.max(1, (int)(range * 0.15f));
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    private class ChartScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float focusX          = detector.getFocusX();
            float dataXBeforeScale = (scrollX + focusX - chartLeft) / nodeWidth;

            nodeWidth *= detector.getScaleFactor();
            nodeWidth  = Math.max(MIN_NODE_WIDTH, Math.min(MAX_NODE_WIDTH, nodeWidth));

            updateScrollBounds();
            scrollX = Math.max(0, Math.min(dataXBeforeScale * nodeWidth - (focusX - chartLeft), maxScrollX));
            invalidate();
            return true;
        }
    }

    private class ChartGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override public boolean onDown(MotionEvent e) {
            if (!scroller.isFinished()) scroller.abortAnimation();
            return true;
        }

        @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            scrollX = Math.max(0, Math.min(scrollX + distanceX, maxScrollX));
            invalidate();
            return true;
        }

        @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            scroller.fling((int) scrollX, 0, -(int) velocityX, 0, 0, (int) maxScrollX, 0, 0);
            invalidate();
            return true;
        }

        @Override public boolean onSingleTapUp(MotionEvent e) {
            return handleTap(e.getX(), e.getY());
        }
    }

    private static class DataPoint {
        final int  value;
        final long timestamp;

        DataPoint(int value, long timestamp) {
            this.value     = value;
            this.timestamp = timestamp;
        }
    }

    public interface OnNodeSelectedListener {
        void onNodeSelected(int index, int value, long timestamp);
        void onNodeDeselected();
    }
}
