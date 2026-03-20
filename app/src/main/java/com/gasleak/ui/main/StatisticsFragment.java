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
package com.gasleak.ui.main;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.gasleak.R;
import com.gasleak.data.api.StatsApiService;
import com.gasleak.data.model.HourlyStatPoint;
import com.gasleak.data.model.RealtimeConfig;
import com.gasleak.data.prefs.SharedPrefs;
import com.gasleak.ui.widget.StatsChartView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class StatisticsFragment extends Fragment {

    private StatsChartView             chartView;
    private LinearLayout               tableContainer;
    private FrameLayout                loadingOverlay;
    private TextView                   tvLoading;
    private android.widget.ScrollView  tableScrollView;

    private RealtimeConfig config;
    private final Handler  mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_statistics, container, false);
        chartView       = root.findViewById(R.id.stats_chart_view);
        tableContainer  = root.findViewById(R.id.table_container);
        loadingOverlay  = root.findViewById(R.id.loading_overlay);
        tvLoading       = root.findViewById(R.id.tv_loading);
        tableScrollView = root.findViewById(R.id.table_scroll_view);
        config = new SharedPrefs(requireContext()).getRealtimeConfig();
        loadStats();
        return root;
    }

    private void loadStats() {
        showLoading(true);
        if (config == null || !config.isValid()) {
            showLoading(false);
            showError(getString(R.string.stat_config_missing));
            return;
        }
        StatsApiService.fetchHourlyStats(config, new StatsApiService.StatsCallback() {
            @Override
            public void onSuccess(final List<HourlyStatPoint> points) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showLoading(false);
                        if (points.isEmpty()) {
                            showError(getString(R.string.stat_no_data, "hourly"));
                            return;
                        }
                        renderChart(points);
                        renderTable(points);
                        if (tableScrollView != null) tableScrollView.scrollTo(0, 0);
                    }
                });
            }
            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showLoading(false);
                        showError(getString(R.string.stat_error, error));
                    }
                });
            }
        });
    }

    private void renderChart(List<HourlyStatPoint> points) {
        if (chartView != null) chartView.setPoints(points);
    }

    private void renderTable(List<HourlyStatPoint> points) {
        if (tableContainer == null) return;
        tableContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm  dd/MM", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        for (HourlyStatPoint p : points) {
            tableContainer.addView(buildDataRow(
                formatBucket(p.getBucket(), sdf),
                String.format(Locale.getDefault(), "%.1f ppm", p.getAvgGas())
            ));
            tableContainer.addView(buildRowDivider());
        }
    }

    private View buildHeaderRow() {
        LinearLayout row = newRow();
        row.setBackgroundColor(0x1A4CAF50);
        TextView tvTime = newCell(getString(R.string.stat_col_time), true);
        TextView tvAvg  = newCell(getString(R.string.stat_col_avg),  true);
        row.addView(tvTime);
        row.addView(tvAvg);
        return row;
    }

    private View buildDataRow(String time, String avg) {
        LinearLayout row = newRow();
        row.addView(newCell(time, false));
        row.addView(newCell(avg, false));
        return row;
    }

    private View buildDivider() {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 2));
        v.setBackgroundColor(0x33FFFFFF);
        return v;
    }

    private View buildRowDivider() {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(0x11FFFFFF);
        return v;
    }

    private LinearLayout newRow() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 22, 0, 22);
        return row;
    }

    private TextView newCell(String text, boolean header) {
        TextView tv = new TextView(requireContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(14f);
        tv.setTextColor(header ? 0xFFFFFFFF : 0xCCFFFFFF);
        if (header) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private String formatBucket(String bucket, SimpleDateFormat sdf) {
        if (bucket == null || bucket.isEmpty()) return "--";
        try {
            String norm = bucket;
            if (norm.endsWith("Z")) norm = norm.substring(0, norm.length() - 1) + "+0000";
            if (norm.length() > 6) {
                String tail = norm.substring(norm.length() - 6);
                if (tail.matches("[+-]\\d{2}:\\d{2}"))
                    norm = norm.substring(0, norm.length() - 6) + tail.replace(":", "");
            }
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = parser.parse(norm);
            return d != null ? sdf.format(d) : bucket;
        } catch (Exception e) {
            return bucket.length() >= 16 ? bucket.substring(0, 16) : bucket;
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        if (tvLoading      != null) tvLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }
}
