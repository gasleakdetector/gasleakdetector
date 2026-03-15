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
package com.gasleak.ui.main;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.gasleak.R;
import com.gasleak.data.api.HistoricalApiService;
import com.gasleak.data.model.HistoricalDataPoint;
import com.gasleak.data.model.RealtimeConfig;
import com.gasleak.data.prefs.SharedPrefs;
import com.gasleak.ui.widget.ChartView;
import java.util.List;

public class StatisticsFragment extends Fragment {

    private ChartView chartView;
    private TextView  tvAvg;
    private TextView  tvMin;
    private TextView  tvMax;
    private TextView  tvSamples;
    private TextView  tvLoading;
    private Button    btn1h;
    private Button    btn6h;
    private Button    btn1d;
    private Button    btn7d;
    private Button    btn30d;
    private View      loadingOverlay;

    private String         currentRange = "1d";
    private RealtimeConfig config;
    private final Handler  mainHandler  = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_statistics, container, false);

        chartView     = root.findViewById(R.id.chart_view);
        tvAvg         = root.findViewById(R.id.tv_avg);
        tvMin         = root.findViewById(R.id.tv_min);
        tvMax         = root.findViewById(R.id.tv_max);
        tvSamples     = root.findViewById(R.id.tv_samples);
        tvLoading     = root.findViewById(R.id.tv_loading);
        loadingOverlay = root.findViewById(R.id.loading_overlay);

        btn1h  = root.findViewById(R.id.btn_range_1h);
        btn6h  = root.findViewById(R.id.btn_range_6h);
        btn1d  = root.findViewById(R.id.btn_range_1d);
        btn7d  = root.findViewById(R.id.btn_range_7d);
        btn30d = root.findViewById(R.id.btn_range_30d);

        btn1h.setOnClickListener(new View.OnClickListener()  { @Override public void onClick(View v) { loadRange("1h"); } });
        btn6h.setOnClickListener(new View.OnClickListener()  { @Override public void onClick(View v) { loadRange("6h"); } });
        btn1d.setOnClickListener(new View.OnClickListener()  { @Override public void onClick(View v) { loadRange("1d"); } });
        btn7d.setOnClickListener(new View.OnClickListener()  { @Override public void onClick(View v) { loadRange("7d"); } });
        btn30d.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { loadRange("30d"); } });

        config = new SharedPrefs(requireContext()).getRealtimeConfig();
        loadRange(currentRange);
        return root;
    }

    private void loadRange(final String range) {
        currentRange = range;
        updateButtonState(range);
        showLoading(true, getString(R.string.stat_loading));

        if (config == null || config.getApiUrl() == null) {
            showError(getString(R.string.stat_config_missing));
            return;
        }

        HistoricalApiService.fetchHistoricalData(config, range,
            new HistoricalApiService.HistoricalDataRangeCallback() {

                @Override
                public void onProgress(final int loaded) {
                    mainHandler.post(new Runnable() {
                        @Override public void run() {
                            showLoading(true, getString(R.string.stat_loading_nodes, loaded));
                        }
                    });
                }

                @Override
                public void onSuccess(final List<HistoricalDataPoint> dataPoints, final String r) {
                    mainHandler.post(new Runnable() {
                        @Override public void run() {
                            showLoading(false, null);
                            if (dataPoints.isEmpty()) {
                                showError(getString(R.string.stat_no_data, r));
                                return;
                            }
                            renderData(dataPoints);
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    mainHandler.post(new Runnable() {
                        @Override public void run() {
                            showLoading(false, null);
                            showError(getString(R.string.stat_error, error));
                        }
                    });
                }
            });
    }

    private void renderData(List<HistoricalDataPoint> dataPoints) {
        if (chartView != null) chartView.setDataPoints(dataPoints);

        double sumAvg = 0;
        double sumMin = Double.MAX_VALUE;
        double sumMax = 0;

        for (HistoricalDataPoint p : dataPoints) {
            sumAvg += p.getGasPpm();
            if (p.getGasPpm() < sumMin) sumMin = p.getGasPpm();
            if (p.getGasPpm() > sumMax) sumMax = p.getGasPpm();
        }
        double avg = dataPoints.isEmpty() ? 0 : sumAvg / dataPoints.size();

        if (tvAvg     != null) tvAvg.setText(getString(R.string.stat_avg, avg));
        if (tvMin     != null) tvMin.setText(getString(R.string.stat_min, sumMin == Double.MAX_VALUE ? 0.0 : sumMin));
        if (tvMax     != null) tvMax.setText(getString(R.string.stat_max, sumMax));
        if (tvSamples != null) tvSamples.setText(getString(R.string.stat_nodes, dataPoints.size()));
    }

    private void showLoading(boolean show, String msg) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        if (tvLoading != null) {
            tvLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            if (msg != null) tvLoading.setText(msg);
        }
    }

    private void showError(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void updateButtonState(String range) {
        setButtonActive(btn1h,  "1h".equals(range));
        setButtonActive(btn6h,  "6h".equals(range));
        setButtonActive(btn1d,  "1d".equals(range));
        setButtonActive(btn7d,  "7d".equals(range));
        setButtonActive(btn30d, "30d".equals(range));
    }

    private void setButtonActive(Button btn, boolean active) {
        if (btn != null) btn.setAlpha(active ? 1.0f : 0.5f);
    }
}
