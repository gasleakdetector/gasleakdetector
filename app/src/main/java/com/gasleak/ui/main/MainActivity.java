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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.gasleak.R;
import com.gasleak.app.GasLeakApplication;
import com.gasleak.data.api.HistoricalApiService;
import com.gasleak.data.local.LocalDataStorage;
import com.gasleak.data.model.GasStatus;
import com.gasleak.data.model.HistoricalDataPoint;
import com.gasleak.data.model.RealtimeConfig;
import com.gasleak.data.prefs.SharedPrefs;
import com.gasleak.data.websocket.WebSocketManager;
import com.gasleak.notification.GasNotificationHelper;
import com.gasleak.service.AppForegroundService;
import com.gasleak.ui.dialog.ConfigDialog;
import com.gasleak.ui.widget.ChartView;
import com.gasleak.ui.widget.CircularGaugeView;
import com.gasleak.util.LocaleHelper;
import com.gasleak.util.ThemeUtil;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements WebSocketManager.Callback, ChartView.OnNodeSelectedListener {

    private static final int  TEXT_ANIMATION_DURATION = 500;
    private static final long NOTIF_COOLDOWN_MS       = 30_000; // 30-second cooldown between repeated alerts

    /* Key for saving/restoring the historical-loaded flag across config changes. */
    private static final String STATE_HISTORICAL_LOADED = "historicalLoaded";

    private CircularGaugeView gaugeView;
    private ChartView         chartView;
    private TextView          gasLevelText;
    private TextView          gasStatusText;
    private TextView          nodeInfoText;
    private int               currentDisplayValue = 0;
    private ValueAnimator     textValueAnimator;

    private ImageButton   btnPlay;
    private SharedPrefs   sharedPrefs;
    private WebSocketManager webSocketManager;
    private boolean       isMonitoring  = false;
    private Handler       mainHandler;
    private PowerManager.WakeLock wakeLock;
    private LocalDataStorage localStorage;
    private List<HistoricalDataPoint> dataPoints;
    private GasLeakApplication app;

    private boolean isNodeLocked       = false;
    private int     selectedNodeIndex  = -1;
    private GasNotificationHelper notificationHelper;
    private int  lastNotifiedLevel     = GasStatus.LEVEL_NORMAL;
    private long lastAlertTimestamp    = 0;

    private View         homePanel;
    private View         statisticsPanel;
    private View         menuPanel;
    private View         menuOverlay;
    private LinearLayout menuItemHome;
    private LinearLayout menuItemStatistics;
    private TextView     menuHomeText;
    private TextView     menuStatisticsText;
    private ImageView    menuHomeIcon;
    private ImageView    menuStatisticsIcon;
    private boolean      isMenuOpen = false;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        String lang = new SharedPrefs(base).getLanguage();
        super.attachBaseContext(LocaleHelper.applyLocale(base, lang));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.applyTheme(this);
        super.onCreate(savedInstanceState);
        sharedPrefs       = new SharedPrefs(this);
        app               = (GasLeakApplication) getApplication();
        notificationHelper = new GasNotificationHelper(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        gaugeView     = findViewById(R.id.gaugeView);
        chartView     = findViewById(R.id.chartView);
        gasLevelText  = findViewById(R.id.gasLevelText);
        gasStatusText = findViewById(R.id.gasStatusText);
        nodeInfoText  = findViewById(R.id.nodeInfoText);
        chartView.setOnNodeSelectedListener(this);

        btnPlay = findViewById(R.id.btn_play);
        webSocketManager = new WebSocketManager(this);
        mainHandler      = new Handler(Looper.getMainLooper());
        localStorage     = new LocalDataStorage(this);
        dataPoints       = new ArrayList<>();

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleMonitoring(); }
        });

        findViewById(R.id.btn_edit).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showConfigDialog(); }
        });

        setupKeepAppRunning();
        setupDrawer();
        updatePlayButton();

        AppForegroundService.start(this);

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

        if (sharedPrefs.getAutoStreamEnabled() && sharedPrefs.hasRealtimeConfig()) {
            mainHandler.postDelayed(new Runnable() {
                @Override public void run() { if (!isMonitoring) startMonitoring(); }
            }, 1000);
        }
    }

    private void setupDrawer() {
        homePanel       = findViewById(R.id.homePanel);
        statisticsPanel = findViewById(R.id.statisticsPanel);
        menuPanel       = findViewById(R.id.menuPanel);
        menuOverlay     = findViewById(R.id.menuOverlay);

        /* Positioned off-screen left; actual offset is set here once the width is known. */
        menuPanel.post(new Runnable() {
            @Override public void run() { menuPanel.setTranslationX(-menuPanel.getWidth()); }
        });

        findViewById(R.id.btn_menu).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openMenu(); }
        });

        menuOverlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { closeMenu(); }
        });

        menuItemHome       = findViewById(R.id.menuItemHome);
        menuItemStatistics = findViewById(R.id.menuItemStatistics);
        menuHomeText       = findViewById(R.id.menuHomeText);
        menuStatisticsText = findViewById(R.id.menuStatisticsText);
        menuHomeIcon       = findViewById(R.id.menuHomeIcon);
        menuStatisticsIcon = findViewById(R.id.menuStatisticsIcon);

        setActiveTab(true);

        menuItemHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homePanel.setVisibility(View.VISIBLE);
                statisticsPanel.setVisibility(View.GONE);
                setActiveTab(true);
                closeMenu();
            }
        });

        menuItemStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homePanel.setVisibility(View.GONE);
                statisticsPanel.setVisibility(View.VISIBLE);
                setActiveTab(false);
                closeMenu();
            }
        });
    }

    private void openMenu() {
        isMenuOpen = true;
        menuOverlay.setVisibility(View.VISIBLE);
        menuOverlay.setAlpha(0f);
        menuPanel.animate().translationX(0).setDuration(250).start();
        menuOverlay.animate().alpha(1f).setDuration(250).start();
    }

    private void closeMenu() {
        isMenuOpen = false;
        menuPanel.animate().translationX(-menuPanel.getWidth()).setDuration(200).start();
        menuOverlay.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
            @Override public void run() { menuOverlay.setVisibility(View.GONE); }
        }).start();
    }

    private void setActiveTab(boolean isHome) {
        int activeColor   = ContextCompat.getColor(this, R.color.colorPrimary);
        int inactiveColor = 0xCCFFFFFF;
        int activeBg      = 0x1A4CAF50;

        menuHomeText.setTextColor(isHome ? activeColor : inactiveColor);
        menuStatisticsText.setTextColor(isHome ? inactiveColor : activeColor);
        menuHomeIcon.setColorFilter(isHome ? activeColor : inactiveColor);
        menuStatisticsIcon.setColorFilter(isHome ? inactiveColor : activeColor);
        menuItemHome.setBackgroundColor(isHome ? activeBg : android.graphics.Color.TRANSPARENT);
        menuItemStatistics.setBackgroundColor(isHome ? android.graphics.Color.TRANSPARENT : activeBg);
        menuHomeText.setTypeface(null, isHome ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        menuStatisticsText.setTypeface(null, isHome ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
    }

    private void setupKeepAppRunning() {
        if (sharedPrefs.getKeepAppRunning()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    getString(R.string.wakelock_tag)
                );
                wakeLock.acquire(10 * 60 * 60 * 1000L);
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        }
    }

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
        Toast.makeText(this, getString(R.string.loading_data), Toast.LENGTH_SHORT).show();
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
        nodeInfoText.setText(localStorage.hasCache()
            ? getString(R.string.network_error_cache)
            : getString(R.string.failed_load_data, error));
    }

    private void updateToLatestNode() {
        if (dataPoints.isEmpty()) return;
        HistoricalDataPoint last = dataPoints.get(dataPoints.size() - 1);
        updateUIAnimated(createStatusFromValue(last.getGasPpm(), last.getTimestamp()));
        SimpleDateFormat fmt = new SimpleDateFormat(getString(R.string.date_format), Locale.ENGLISH);
        nodeInfoText.setText(getString(R.string.value_at_time, last.getGasPpm(), fmt.format(new Date(last.getTimestamp()))));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (menu instanceof MenuBuilder) ((MenuBuilder) menu).setOptionalIconsVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menuSettings) {
            startActivity(new Intent(this, SettingActivity.class));
            overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out);
            return true;
        } else if (id == R.id.menuAbout) {
            startActivity(new Intent(this, InfoActivity.class));
            overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out);
            return true;
        } else if (id == R.id.menuReset) {
            showResetDataDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showResetDataDialog() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.reset_data_title))
            .setMessage(getString(R.string.reset_data_message))
            .setPositiveButton(getString(R.string.btn_reset), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    localStorage.clearCache();
                    chartView.clearData();
                    dataPoints.clear();
                    app.setCachedNodes(null);
                    app.setHistoricalDataLoaded(false);
                    sharedPrefs.clearFetchTime();
                    gaugeView.setValue(0);
                    gasLevelText.setText(getString(R.string.status_placeholder));
                    gasStatusText.setText(getString(R.string.status_full_placeholder));
                    loadHistoricalData();
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    private void showConfigDialog() {
        RealtimeConfig currentConfig = sharedPrefs.getRealtimeConfig();
        new ConfigDialog(this, currentConfig, new ConfigDialog.OnConfigSavedListener() {
            @Override
            public void onConfigSaved(RealtimeConfig config) {
                sharedPrefs.saveRealtimeConfig(config);
                Toast.makeText(MainActivity.this, getString(R.string.config_saved), Toast.LENGTH_SHORT).show();
                app.setHistoricalDataLoaded(false);
                app.setCachedNodes(null);
                chartView.clearData();
                dataPoints.clear();
                loadHistoricalData();
                if (isMonitoring) stopMonitoring();
                if (sharedPrefs.getAutoStreamEnabled()) {
                    mainHandler.postDelayed(new Runnable() {
                        @Override public void run() { startMonitoring(); }
                    }, 500);
                }
            }
        }).show();
    }

    private void toggleMonitoring() {
        if (isMonitoring) stopMonitoring();
        else startMonitoring();
    }

    private void startMonitoring() {
        RealtimeConfig config = sharedPrefs.getRealtimeConfig();
        if (config == null || !config.isValid()) {
            Toast.makeText(this, getString(R.string.config_required), Toast.LENGTH_SHORT).show();
            showConfigDialog();
            return;
        }
        webSocketManager.connect(config);
    }

    private void stopMonitoring() {
        webSocketManager.disconnect();
        isMonitoring = false;
        updatePlayButton();
    }

    private void updatePlayButton() {
        if (isMonitoring) {
            btnPlay.setImageResource(R.drawable.ic_stop);
            btnPlay.setContentDescription(getString(R.string.stop_content_description));
        } else {
            btnPlay.setImageResource(R.drawable.ic_play);
            btnPlay.setContentDescription(getString(R.string.play_content_description));
        }
    }

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean wasMonitoring = isMonitoring;
                isMonitoring = true;
                updatePlayButton();
                /* Only show the toast on the first connection; suppress it during auto-reconnects. */
                if (!wasMonitoring) {
                    Toast.makeText(MainActivity.this, getString(R.string.connected), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /* Don't set isMonitoring = false here — the socket reconnects automatically.
                 * Only flip the flag when the user explicitly presses Stop. */
                updatePlayButton();
            }
        });
    }

    @Override
    public void onDataReceived(final int gasPpm, final String status, final String timestamp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chartView.addDataPoint(gasPpm);

                HistoricalDataPoint newPoint = new HistoricalDataPoint();
                newPoint.setGasPpm(gasPpm);
                newPoint.setStatus(status);
                newPoint.setCreatedAt(timestamp.isEmpty()
                    ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date())
                    : timestamp);
                dataPoints.add(newPoint);
                app.setCachedNodes(dataPoints);

                if (!isNodeLocked) {
                    updateUIAnimated(createStatusFromValue(gasPpm, newPoint.getTimestamp()));
                    SimpleDateFormat fmt = new SimpleDateFormat(getString(R.string.date_format), Locale.ENGLISH);
                    nodeInfoText.setText(getString(R.string.value_at_time, gasPpm, fmt.format(new Date(newPoint.getTimestamp()))));
                }

                /* Send an alert notification when gas level is above normal. */
                GasStatus liveStatus = createStatusFromValue(gasPpm, newPoint.getTimestamp());
                if (sharedPrefs.getNotificationsEnabled() && !liveStatus.isNormal()) {
                    long now             = System.currentTimeMillis();
                    boolean escalated    = liveStatus.getLevel() > lastNotifiedLevel;
                    boolean cooldownDone = (now - lastAlertTimestamp) >= NOTIF_COOLDOWN_MS;
                    if (escalated || cooldownDone) {
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
        });
    }

    @Override
    public void onError(final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, getString(R.string.error_prefix, error), Toast.LENGTH_SHORT).show();
                isMonitoring = false;
                updatePlayButton();
            }
        });
    }

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
            SimpleDateFormat fmt = new SimpleDateFormat(getString(R.string.date_format), Locale.ENGLISH);
            nodeInfoText.setText(getString(R.string.value_at_time, value, fmt.format(new Date(timestamp))));
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

    private GasStatus createStatusFromValue(int value, long timestamp) {
        int level = GasStatus.calculateLevel(value);
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
        int currentColorRes = getStatusColorRes(GasStatus.calculateLevel(currentDisplayValue));
        int targetColorRes  = getStatusColorRes(status.getLevel());
        int currentColor    = ContextCompat.getColor(this, currentColorRes);
        int targetColor     = ContextCompat.getColor(this, targetColorRes);

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

    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_HISTORICAL_LOADED, app.isHistoricalDataLoaded());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupKeepAppRunning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textValueAnimator != null) textValueAnimator.cancel();
        if (webSocketManager  != null) webSocketManager.destroy();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
}
