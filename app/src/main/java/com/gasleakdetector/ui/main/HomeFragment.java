/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-05-23
 */
package com.gasleakdetector.ui.main;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.gasleakdetector.R;
import com.gasleakdetector.app.GasLeakApplication;
import com.gasleakdetector.data.api.HistoricalApiService;
import com.gasleakdetector.data.local.LocalDataStorage;
import com.gasleakdetector.data.model.GasStatus;
import com.gasleakdetector.data.model.HistoricalDataPoint;
import com.gasleakdetector.data.model.RealtimeConfig;
import com.gasleakdetector.data.prefs.SharedPrefs;
import com.gasleakdetector.data.websocket.WebSocketManager;
import com.gasleakdetector.notification.GasNotificationHelper;
import com.gasleakdetector.ui.widget.ChartView;
import com.gasleakdetector.ui.widget.CircularGaugeView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HomeFragment — live gas reading panel.
 *
 * Owns the circular gauge, real-time chart, and node info label.
 * WebSocket lifecycle (connect / disconnect) is delegated to MainActivity
 * via the {@link Host} interface so the Activity can coordinate toolbar state.
 */
public class HomeFragment extends Fragment
        implements WebSocketManager.Callback, ChartView.OnNodeSelectedListener {

    /* ------------------------------------------------------------------ */
    /*  Host interface — implemented by MainActivity                       */
    /* ------------------------------------------------------------------ */

    /** Callbacks that MainActivity must implement to coordinate with this fragment. */
    public interface Host {
        /** Fragment requests the monitoring stream to start. */
        void onStartMonitoringRequested();
        /** Fragment requests the monitoring stream to stop. */
        void onStopMonitoringRequested();
        /** Fragment reports that isMonitoring state changed so toolbar can update. */
        void onMonitoringStateChanged(boolean isMonitoring);
    }

    /* ------------------------------------------------------------------ */
    /*  Constants                                                           */
    /* ------------------------------------------------------------------ */

    private static final int    TEXT_ANIMATION_DURATION = 500;
    private static final int    MAX_NODES               = 1000;
    private static final long   NOTIF_COOLDOWN_MS       = 30_000;
    private static final String DEFAULT_DEVICE_ID       = "ESP_GASLEAK_01";

    /* ------------------------------------------------------------------ */
    /*  Views                                                               */
    /* ------------------------------------------------------------------ */

    private CircularGaugeView gaugeView;
    private ChartView         chartView;
    private TextView          gasLevelText;
    private TextView          gasStatusText;
    private TextView          nodeInfoText;

    /* ------------------------------------------------------------------ */
    /*  State                                                               */
    /* ------------------------------------------------------------------ */

    private int           currentDisplayValue = 0;
    private ValueAnimator textValueAnimator;
    private boolean       isNodeLocked        = false;
    private int           selectedNodeIndex   = -1;
    private int           lastNotifiedLevel   = GasStatus.LEVEL_NORMAL;
    private long          lastAlertTimestamp  = 0;

    /* ------------------------------------------------------------------ */
    /*  Dependencies                                                        */
    /* ------------------------------------------------------------------ */

    private SharedPrefs              sharedPrefs;
    private GasLeakApplication       app;
    private LocalDataStorage         localStorage;
    private GasNotificationHelper    notificationHelper;
    private List<HistoricalDataPoint> dataPoints;
    private Handler                  mainHandler;

    /* ------------------------------------------------------------------ */
    /*  Lifecycle                                                           */
    /* ------------------------------------------------------------------ */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPrefs        = new SharedPrefs(requireContext());
        app                = (GasLeakApplication) requireActivity().getApplication();
        localStorage       = new LocalDataStorage(requireContext());
        notificationHelper = new GasNotificationHelper(requireContext());
        dataPoints         = new ArrayList<>();
        mainHandler        = new Handler(Looper.getMainLooper());

        gaugeView     = view.findViewById(R.id.gaugeView);
        chartView     = view.findViewById(R.id.chartView);
        gasLevelText  = view.findViewById(R.id.gasLevelText);
        gasStatusText = view.findViewById(R.id.gasStatusText);
        nodeInfoText  = view.findViewById(R.id.nodeInfoText);

        chartView.setOnNodeSelectedListener(this);

        if (app.hasInMemoryData()) {
            dataPoints.clear();
            dataPoints.addAll(app.getCachedNodes());
            for (HistoricalDataPoint point : dataPoints) {
                chartView.addDataPointWithTimestamp(point.getGasPpm(), point.getTimestamp());
            }
            if (!dataPoints.isEmpty()) {
                HistoricalDataPoint last = dataPoints.get(dataPoints.size() - 1);
                updateUIAnimated(createStatusFromValue(last.getGasPpm(), last.getTimestamp()));
            }
            nodeInfoText.setText(localStorage.getCacheInfo() + getString(R.string.from_cache));
        } else {
            loadCachedData();
            loadHistoricalData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (textValueAnimator != null) textValueAnimator.cancel();
    }

    /* ------------------------------------------------------------------ */
    /*  Public API — called by MainActivity                                */
    /* ------------------------------------------------------------------ */

    /** Called by MainActivity when WebSocket connects successfully. */
    public void onConnectedExternal() {
        gasStatusText.post(new Runnable() {
            @Override public void run() {
                Host host = getHost();
                if (host != null) host.onMonitoringStateChanged(true);
            }
        });
    }

    /** Called by MainActivity when WebSocket disconnects. */
    public void onDisconnectedExternal() {
        gasStatusText.post(new Runnable() {
            @Override public void run() {
                Host host = getHost();
                if (host != null) host.onMonitoringStateChanged(false);
            }
        });
    }

    /** Called by MainActivity to push a live WebSocket reading into the fragment. */
    public void onDataReceivedExternal(int gasPpm, String status, String timestamp, String deviceId) {
        HistoricalDataPoint newPoint = new HistoricalDataPoint();
        newPoint.setGasPpm(gasPpm);
        newPoint.setStatus(status);
        if (deviceId != null && !deviceId.isEmpty()) {
            newPoint.setDeviceId(deviceId);
        } else {
            RealtimeConfig cfg = sharedPrefs.getRealtimeConfig();
            String id = (cfg != null && cfg.getDeviceId() != null && !cfg.getDeviceId().isEmpty())
                    ? cfg.getDeviceId() : DEFAULT_DEVICE_ID;
            newPoint.setDeviceId(id);
        }
        newPoint.setCreatedAt(timestamp.isEmpty()
            ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date())
            : timestamp);

        chartView.addDataPointWithTimestamp(gasPpm, newPoint.getTimestamp());
        dataPoints.add(newPoint);
        if (dataPoints.size() > MAX_NODES) {
            dataPoints.subList(0, dataPoints.size() - MAX_NODES).clear();
        }
        app.setCachedNodes(dataPoints);

        if (!isNodeLocked) {
            updateUIAnimated(createStatusFromValue(gasPpm, newPoint.getTimestamp()));
            SimpleDateFormat fmt = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
            String displayId = (newPoint.getDeviceId() != null) ? newPoint.getDeviceId() : DEFAULT_DEVICE_ID;
            nodeInfoText.setText(getString(R.string.value_at_time, gasPpm, fmt.format(new Date(newPoint.getTimestamp())), displayId));
        }

        GasStatus liveStatus = createStatusFromValue(gasPpm, newPoint.getTimestamp());
        if (sharedPrefs.getNotificationsEnabled() && !liveStatus.isNormal()) {
            long now          = System.currentTimeMillis();
            boolean escalated = liveStatus.getLevel() > lastNotifiedLevel;
            boolean cooldown  = (now - lastAlertTimestamp) >= NOTIF_COOLDOWN_MS;
            if (escalated || cooldown) {
                notificationHelper.showAlert(liveStatus);
                lastNotifiedLevel  = liveStatus.getLevel();
                lastAlertTimestamp = now;
            }
        } else if (sharedPrefs.getNotificationsEnabled()) {
            lastNotifiedLevel = GasStatus.LEVEL_NORMAL;
        }

        final HistoricalDataPoint pointToSave = newPoint;
        new Thread(new Runnable() {
            @Override public void run() { localStorage.addNode(pointToSave); }
        }).start();
    }

    /** Called by MainActivity when an error occurs on the WebSocket. */
    public void onErrorExternal(String error) {
        Host host = getHost();
        if (host != null) host.onMonitoringStateChanged(false);
    }

    /** Reload data after config change. */
    public void reloadAfterConfigChange() {
        app.setHistoricalDataLoaded(false);
        app.setCachedNodes(null);
        chartView.clearData();
        dataPoints.clear();
        loadHistoricalData();
    }

    /* ------------------------------------------------------------------ */
    /*  WebSocketManager.Callback (not used directly — proxied via Host)   */
    /* ------------------------------------------------------------------ */

    @Override public void onConnected()   { /* proxied via onConnectedExternal()   */ }
    @Override public void onDisconnected(){ /* proxied via onDisconnectedExternal() */ }

    @Override
    public void onDataReceived(int gasPpm, String status, String timestamp, String deviceId) {
        /* proxied via onDataReceivedExternal() */
    }

    @Override
    public void onError(String error) { /* proxied via onErrorExternal() */ }

    /* ------------------------------------------------------------------ */
    /*  ChartView.OnNodeSelectedListener                                   */
    /* ------------------------------------------------------------------ */

    @Override
    public void onNodeSelected(int index, int value, long timestamp) {
        if (isNodeLocked && selectedNodeIndex == index) {
            isNodeLocked      = false;
            selectedNodeIndex = -1;
            chartView.clearSelection();
            updateToLatestNode();
        } else {
            isNodeLocked      = true;
            selectedNodeIndex = index;
            SimpleDateFormat fmt = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
            String selectedDeviceId = getDefaultDeviceId();
            if (index >= 0 && index < dataPoints.size()) {
                String id = dataPoints.get(index).getDeviceId();
                if (id != null && !id.isEmpty()) selectedDeviceId = id;
            }
            nodeInfoText.setText(getString(R.string.value_at_time, value, fmt.format(new Date(timestamp)), selectedDeviceId));
            updateUIAnimated(createStatusFromValue(value, timestamp));
        }
    }

    @Override
    public void onNodeDeselected() {
        if (isNodeLocked) {
            isNodeLocked      = false;
            selectedNodeIndex = -1;
            updateToLatestNode();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Private helpers                                                     */
    /* ------------------------------------------------------------------ */

    private void loadCachedData() {
        if (!localStorage.hasCache()) {
            nodeInfoText.setText(getString(R.string.no_cached_data));
            return;
        }
        List<HistoricalDataPoint> cached = localStorage.loadNodes();
        if (cached.isEmpty()) {
            nodeInfoText.setText(getString(R.string.cache_empty));
            return;
        }
        for (HistoricalDataPoint point : cached) {
            if (point.getDeviceId() == null || point.getDeviceId().isEmpty()) {
                point.setDeviceId(getDefaultDeviceId());
            }
            chartView.addDataPointWithTimestamp(point.getGasPpm(), point.getTimestamp());
        }
        dataPoints.clear();
        dataPoints.addAll(cached);
        if (!dataPoints.isEmpty()) {
            HistoricalDataPoint last = dataPoints.get(dataPoints.size() - 1);
            updateUIAnimated(createStatusFromValue(last.getGasPpm(), last.getTimestamp()));
        }
        nodeInfoText.setText(localStorage.getCacheInfo() + getString(R.string.from_cache));
    }

    private void loadHistoricalData() {
        RealtimeConfig config = sharedPrefs.getRealtimeConfig();
        if (config == null || !config.isValid()) {
            nodeInfoText.setText(localStorage.hasCache()
                ? localStorage.getCacheInfo() + getString(R.string.offline_mode)
                : getString(R.string.configure_api_prompt));
            return;
        }
        if (app.isHistoricalDataLoaded()) return;
        nodeInfoText.setText(getString(R.string.loading_historical));
        HistoricalApiService.fetchHistoricalData(config, new HistoricalApiService.HistoricalDataCallback() {
            @Override
            public void onSuccess(final List<HistoricalDataPoint> points) {
                mainHandler.post(new Runnable() {
                    @Override public void run() { handleHistoricalDataSuccess(points); }
                });
            }
            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override public void run() { handleHistoricalDataError(error); }
                });
            }
        });
    }

    private void handleHistoricalDataSuccess(List<HistoricalDataPoint> points) {
        app.setHistoricalDataLoaded(true);
        if (points == null || points.isEmpty()) {
            RealtimeConfig cfg = sharedPrefs.getRealtimeConfig();
            if (cfg != null && cfg.getDeviceId() != null && !cfg.getDeviceId().isEmpty()) {
                RealtimeConfig cfgNoDevice = new RealtimeConfig(cfg.getApiUrl(), cfg.getApiKey(), "");
                HistoricalApiService.fetchHistoricalData(cfgNoDevice,
                    new HistoricalApiService.HistoricalDataCallback() {
                        @Override public void onSuccess(final List<HistoricalDataPoint> pts) {
                            mainHandler.post(new Runnable() {
                                @Override public void run() { handleHistoricalDataFallback(pts); }
                            });
                        }
                        @Override public void onError(final String err) {
                            mainHandler.post(new Runnable() {
                                @Override public void run() {
                                    if (nodeInfoText != null)
                                        nodeInfoText.setText(getString(R.string.no_historical_data));
                                }
                            });
                        }
                    });
                return;
            }
            chartView.clearData();
            dataPoints.clear();
            gaugeView.setValue(0);
            gasLevelText.setText(getString(R.string.status_placeholder));
            gasStatusText.setText(getString(R.string.status_full_placeholder));
            nodeInfoText.setText(getString(R.string.no_historical_data));
            return;
        }
        chartView.clearData();
        for (HistoricalDataPoint point : points) {
            if (point.getDeviceId() == null || point.getDeviceId().isEmpty()) {
                point.setDeviceId(getDefaultDeviceId());
            }
            chartView.addDataPointWithTimestamp(point.getGasPpm(), point.getTimestamp());
        }
        dataPoints.clear();
        dataPoints.addAll(points);
        app.setCachedNodes(dataPoints);
        localStorage.saveNodes(points);
        sharedPrefs.markFetchTime();
        updateToLatestNode();
        nodeInfoText.setText(getString(R.string.loaded_data_points, points.size()));
    }

    private void handleHistoricalDataFallback(List<HistoricalDataPoint> points) {
        if (points == null || points.isEmpty()) {
            nodeInfoText.setText(getString(R.string.no_historical_data));
            return;
        }
        app.setHistoricalDataLoaded(true);
        chartView.clearData();
        for (HistoricalDataPoint point : points) {
            if (point.getDeviceId() == null || point.getDeviceId().isEmpty()) {
                point.setDeviceId(getDefaultDeviceId());
            }
            chartView.addDataPointWithTimestamp(point.getGasPpm(), point.getTimestamp());
        }
        dataPoints.clear();
        dataPoints.addAll(points);
        app.setCachedNodes(dataPoints);
        localStorage.saveNodes(points);
        sharedPrefs.markFetchTime();
        updateToLatestNode();
        nodeInfoText.setText(getString(R.string.loaded_data_points, points.size()));
    }

    private void handleHistoricalDataError(String error) {
        if (nodeInfoText == null) return;
        nodeInfoText.setText(localStorage.hasCache()
            ? getString(R.string.network_error_cache)
            : getString(R.string.failed_load_data, error));
    }

    private void updateToLatestNode() {
        if (dataPoints.isEmpty()) return;
        HistoricalDataPoint last = dataPoints.get(dataPoints.size() - 1);
        updateUIAnimated(createStatusFromValue(last.getGasPpm(), last.getTimestamp()));
        SimpleDateFormat fmt    = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
        String deviceId         = (last.getDeviceId() != null && !last.getDeviceId().isEmpty())
                ? last.getDeviceId() : getDefaultDeviceId();
        nodeInfoText.setText(getString(R.string.value_at_time, last.getGasPpm(), fmt.format(new Date(last.getTimestamp())), deviceId));
    }

    private GasStatus createStatusFromValue(int value, long timestamp) {
        int warningThreshold = sharedPrefs.getWarningThreshold();
        int dangerThreshold  = sharedPrefs.getDangerThreshold();
        int level = GasStatus.calculateLevel(value, warningThreshold, dangerThreshold);
        return new GasStatus(level, value, timestamp, generateMessage(level, value));
    }

    private String generateMessage(int level, int value) {
        switch (level) {
            case GasStatus.LEVEL_NORMAL:  return getString(R.string.msg_normal);
            case GasStatus.LEVEL_WARNING: return getString(R.string.msg_warning, value);
            case GasStatus.LEVEL_DANGER:  return getString(R.string.msg_danger, value);
            default:                      return getString(R.string.msg_unknown);
        }
    }

    private void updateUIAnimated(GasStatus status) {
        animateValueText(status.getConcentration());
        animateStatusText(status);
        gaugeView.setValue(status.getConcentration());
    }

    private void animateValueText(final int targetValue) {
        if (textValueAnimator != null) textValueAnimator.cancel();
        textValueAnimator = ValueAnimator.ofInt(currentDisplayValue, targetValue);
        textValueAnimator.setDuration(TEXT_ANIMATION_DURATION);
        textValueAnimator.setInterpolator(new DecelerateInterpolator());
        textValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                currentDisplayValue = (int) animation.getAnimatedValue();
                gasLevelText.setText(String.valueOf(currentDisplayValue));
            }
        });
        textValueAnimator.start();
    }

    private void animateStatusText(final GasStatus status) {
        int currentColor = ContextCompat.getColor(requireContext(), getStatusColorRes(GasStatus.calculateLevel(currentDisplayValue)));
        int targetColor  = ContextCompat.getColor(requireContext(), getStatusColorRes(status.getLevel()));

        ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor, targetColor);
        colorAnimator.setDuration(TEXT_ANIMATION_DURATION);
        colorAnimator.setInterpolator(new DecelerateInterpolator());
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                gasStatusText.setTextColor((int) animation.getAnimatedValue());
            }
        });
        colorAnimator.start();

        int statusResId;
        switch (status.getLevel()) {
            case GasStatus.LEVEL_WARNING: statusResId = R.string.status_warning; break;
            case GasStatus.LEVEL_DANGER:  statusResId = R.string.status_danger;  break;
            case GasStatus.LEVEL_NORMAL:  statusResId = R.string.status_normal;  break;
            default:                      statusResId = R.string.status_unknown;  break;
        }
        gasStatusText.setText(getString(R.string.status_prefix) + getString(statusResId));
    }

    private int getStatusColorRes(int level) {
        switch (level) {
            case GasStatus.LEVEL_NORMAL:  return R.color.statusNormal;
            case GasStatus.LEVEL_WARNING: return R.color.statusWarning;
            case GasStatus.LEVEL_DANGER:  return R.color.statusDanger;
            default:                      return R.color.statusNormal;
        }
    }

    private String getDefaultDeviceId() {
        RealtimeConfig cfg = sharedPrefs.getRealtimeConfig();
        if (cfg != null && cfg.getDeviceId() != null && !cfg.getDeviceId().isEmpty()) {
            return cfg.getDeviceId();
        }
        return DEFAULT_DEVICE_ID;
    }

    private Host getHost() {
        if (getActivity() instanceof Host) return (Host) getActivity();
        return null;
    }
}
