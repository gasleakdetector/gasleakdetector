/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-06-15
 */
package com.gasleakdetector.ui.main;

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
import com.gasleakdetector.R;
import com.gasleakdetector.data.api.StatsApiService;
import com.gasleakdetector.data.local.StatsLocalStorage;
import com.gasleakdetector.data.model.HourlyStatPoint;
import com.gasleakdetector.data.model.RealtimeConfig;
import com.gasleakdetector.data.prefs.SharedPrefs;
import com.gasleakdetector.ui.widget.StatsChartView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class StatisticsFragment extends Fragment {

    /* ------------------------------------------------------------------ */
    /*  Views                                                               */
    /* ------------------------------------------------------------------ */

    private StatsChartView            chartView;
    private LinearLayout              tableContainer;
    private FrameLayout               loadingOverlay;
    private TextView                  tvLoading;
    private android.widget.ScrollView tableScrollView;

    /* ------------------------------------------------------------------ */
    /*  Dependencies                                                        */
    /* ------------------------------------------------------------------ */

    private RealtimeConfig    config;
    private StatsLocalStorage statsStorage;
    private final Handler     mainHandler = new Handler(Looper.getMainLooper());

    /* ------------------------------------------------------------------ */
    /*  Lifecycle                                                           */
    /* ------------------------------------------------------------------ */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_statistics, container, false);
        chartView       = root.findViewById(R.id.stats_chart_view);
        tableContainer  = root.findViewById(R.id.table_container);
        loadingOverlay  = root.findViewById(R.id.loading_overlay);
        tvLoading       = root.findViewById(R.id.tv_loading);
        tableScrollView = root.findViewById(R.id.table_scroll_view);

        SharedPrefs prefs = new SharedPrefs(requireContext());
        config       = prefs.getRealtimeConfig();
        statsStorage = new StatsLocalStorage(requireContext());

        loadStats();
        return root;
    }

    /* ------------------------------------------------------------------ */
    /*  Load - cache first, then network                                    */
    /* ------------------------------------------------------------------ */

    private void loadStats() {
        if (config == null || !config.isValid()) {
            showError(getString(R.string.stat_config_missing));
            return;
        }

        // Show cached data immediately while network loads
        if (statsStorage.hasCache()) {
            List<HourlyStatPoint> cached = statsStorage.loadStats();
            if (!cached.isEmpty()) {
                renderChart(cached);
                renderTable(cached);
                if (tableScrollView != null) tableScrollView.scrollTo(0, 0);
            }
        }

        showLoading(true);
        fetchFromNetwork();
    }

    private void fetchFromNetwork() {
        StatsApiService.fetchHourlyStats(config, new StatsApiService.StatsCallback() {
            @Override
            public void onSuccess(final List<HourlyStatPoint> points) {
                // Save to cache on background thread before posting to UI
                if (!points.isEmpty()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            statsStorage.saveStats(points);
                        }
                    }).start();
                }
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isSafe()) return;
                        showLoading(false);
                        if (points.isEmpty()) {
                            // Keep cached data visible if network returns empty
                            if (!statsStorage.hasCache()) {
                                showError(getString(R.string.stat_no_data, "hourly"));
                            }
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
                        if (!isSafe()) return;
                        showLoading(false);
                        if (statsStorage.hasCache()) {
                            Toast.makeText(requireContext(),
                                    getString(R.string.network_error_cache), Toast.LENGTH_SHORT).show();
                        } else {
                            showError(getString(R.string.stat_error, error));
                        }
                    }
                });
            }
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Render                                                              */
    /* ------------------------------------------------------------------ */

    private void renderChart(List<HourlyStatPoint> points) {
        if (chartView != null) chartView.setPoints(points);
    }

    private void renderTable(List<HourlyStatPoint> points) {
        if (tableContainer == null) return;
        tableContainer.removeAllViews();
        List<HourlyStatPoint> sorted = new ArrayList<>(points);
        Collections.reverse(sorted);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm  dd/MM", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        for (HourlyStatPoint p : sorted) {
            tableContainer.addView(buildDataRow(
                formatBucket(p.getBucket(), sdf),
                String.format(Locale.getDefault(), "%.1f ppm", p.getAvgGas())
            ));
            tableContainer.addView(buildRowDivider());
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Private - row builders                                              */
    /* ------------------------------------------------------------------ */

    private View buildDataRow(String time, String avg) {
        LinearLayout row = newRow();
        row.addView(newCell(time, false));
        row.addView(newCell(avg, false));
        return row;
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

    /* ------------------------------------------------------------------ */
    /*  Private - helpers                                                   */
    /* ------------------------------------------------------------------ */

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

    /** Check Fragment is still attached before touching views. */
    private boolean isSafe() {
        return isAdded() && getActivity() != null;
    }
}
