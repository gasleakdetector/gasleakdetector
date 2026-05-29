/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-05-28
 */
package com.gasleakdetector.ui.main;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.gasleakdetector.R;
import com.gasleakdetector.data.model.RealtimeConfig;
import com.gasleakdetector.data.prefs.SharedPrefs;
import com.gasleakdetector.data.websocket.WebSocketManager;
import com.gasleakdetector.service.AppForegroundService;
import com.gasleakdetector.ui.dialog.ConfigDialog;
import com.gasleakdetector.util.LocaleHelper;
import com.gasleakdetector.util.ThemeUtil;

/**
 * MainActivity: shell activity.
 *
 * Responsibilities:
 *  - Toolbar (play/stop button, edit config button, overflow menu)
 *  - Side drawer navigation (Home ↔ Statistics)
 *  - WebSocket lifecycle (connect / disconnect)
 *  - WakeLock and keep-screen-on
 *  - Foreground service start
 *
 * All home-screen UI (gauge, chart, node info) lives in {@link HomeFragment}.
 * Statistics UI lives in {@link StatisticsFragment}.
 */
public class MainActivity extends AppCompatActivity
        implements WebSocketManager.Callback, HomeFragment.Host {

    private static final String FEEDBACK_EMAIL = "pan2512811@gmail.com";

    /* ------------------------------------------------------------------ */
    /*  Views                                                               */
    /* ------------------------------------------------------------------ */

    private ImageButton btnPlay;
    private View        homePanel;
    private View        statisticsPanel;
    private View        menuPanel;
    private View        menuOverlay;
    private LinearLayout menuItemHome;
    private LinearLayout menuItemStatistics;
    private TextView    menuHomeText;
    private TextView    menuStatisticsText;
    private ImageView   menuHomeIcon;
    private ImageView   menuStatisticsIcon;

    /* ------------------------------------------------------------------ */
    /*  State                                                               */
    /* ------------------------------------------------------------------ */

    private boolean isMonitoring      = false;
    private boolean statisticsLoaded  = false;

    /* ------------------------------------------------------------------ */
    /*  Dependencies                                                        */
    /* ------------------------------------------------------------------ */

    private SharedPrefs      sharedPrefs;
    private WebSocketManager webSocketManager;
    private Handler          mainHandler;
    private PowerManager.WakeLock wakeLock;

    /* ------------------------------------------------------------------ */
    /*  Lifecycle                                                           */
    /* ------------------------------------------------------------------ */

    @Override
    protected void attachBaseContext(android.content.Context base) {
        String lang = new SharedPrefs(base).getLanguage();
        super.attachBaseContext(LocaleHelper.applyLocale(base, lang));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.applyTheme(this);
        super.onCreate(savedInstanceState);
        sharedPrefs      = new SharedPrefs(this);
        webSocketManager = new WebSocketManager(this);
        mainHandler      = new Handler(Looper.getMainLooper());

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnPlay = findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleMonitoring(); }
        });

        findViewById(R.id.btn_edit).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showConfigDialog(); }
        });

        setupDrawer();
        updatePlayButton();
        requestNotificationPermission();
        setupKeepAppRunning();

        AppForegroundService.start(this);

        /* Load HomeFragment on first create only. */
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.homePanel, new HomeFragment())
                .commit();
        }

        if (sharedPrefs.getAutoStreamEnabled() && sharedPrefs.hasRealtimeConfig()) {
            mainHandler.postDelayed(new Runnable() {
                @Override public void run() { if (!isMonitoring) startMonitoring(); }
            }, 1000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupKeepAppRunning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketManager != null) webSocketManager.destroy();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    /* ------------------------------------------------------------------ */
    /*  HomeFragment.Host                                                   */
    /* ------------------------------------------------------------------ */

    @Override
    public void onStartMonitoringRequested() { startMonitoring(); }

    @Override
    public void onStopMonitoringRequested()  { stopMonitoring(); }

    @Override
    public void onMonitoringStateChanged(boolean monitoring) {
        isMonitoring = monitoring;
        updatePlayButton();
    }

    /* ------------------------------------------------------------------ */
    /*  WebSocketManager.Callback                                          */
    /* ------------------------------------------------------------------ */

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean was = isMonitoring;
                isMonitoring = true;
                updatePlayButton();
                if (!was) Toast.makeText(MainActivity.this, getString(R.string.connected), Toast.LENGTH_SHORT).show();
                HomeFragment frag = getHomeFragment();
                if (frag != null) frag.onConnectedExternal();
            }
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isMonitoring = false;
                updatePlayButton();
                HomeFragment frag = getHomeFragment();
                if (frag != null) frag.onDisconnectedExternal();
            }
        });
    }

    @Override
    public void onDataReceived(final int gasPpm, final String status, final String timestamp, final String deviceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                HomeFragment frag = getHomeFragment();
                if (frag != null) frag.onDataReceivedExternal(gasPpm, status, timestamp, deviceId);
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
                HomeFragment frag = getHomeFragment();
                if (frag != null) frag.onErrorExternal(error);
            }
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Menu                                                                */
    /* ------------------------------------------------------------------ */

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
        } else if (id == R.id.menuFeedback) {
            openFeedbackEmail();
            return true;
        } else if (id == R.id.menuReset) {
            showResetDataDialog();
            return true;
        } else if (id == R.id.menuAbout) {
            startActivity(new Intent(this, InfoActivity.class));
            overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* ------------------------------------------------------------------ */
    /*  Private - monitoring                                                */
    /* ------------------------------------------------------------------ */

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

    /* ------------------------------------------------------------------ */
    /*  Private - drawer                                                    */
    /* ------------------------------------------------------------------ */

    private void setupDrawer() {
        homePanel       = findViewById(R.id.homePanel);
        statisticsPanel = findViewById(R.id.statisticsPanel);
        menuPanel       = findViewById(R.id.menuPanel);
        menuOverlay     = findViewById(R.id.menuOverlay);

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
                if (!statisticsLoaded) {
                    statisticsLoaded = true;
                    getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.statisticsPanel, new StatisticsFragment())
                        .commit();
                }
            }
        });
    }

    private void openMenu() {
        menuOverlay.setVisibility(View.VISIBLE);
        menuOverlay.setAlpha(0f);
        menuPanel.animate().translationX(0).setDuration(250).start();
        menuOverlay.animate().alpha(1f).setDuration(250).start();
    }

    private void closeMenu() {
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

    /* ------------------------------------------------------------------ */
    /*  Private - misc                                                      */
    /* ------------------------------------------------------------------ */

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void setupKeepAppRunning() {
        if (sharedPrefs.getKeepAppRunning()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.wakelock_tag));
                wakeLock.acquire();
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); wakeLock = null; }
        }
    }

    private void showConfigDialog() {
        RealtimeConfig current = sharedPrefs.getRealtimeConfig();
        new ConfigDialog(this, current, new ConfigDialog.OnConfigSavedListener() {
            @Override
            public void onConfigSaved(RealtimeConfig newConfig) {
                boolean changed = !newConfig.hasSameParams(current);
                sharedPrefs.saveRealtimeConfig(newConfig);
                Toast.makeText(MainActivity.this, getString(R.string.config_saved), Toast.LENGTH_SHORT).show();
                if (!changed) return;
                HomeFragment frag = getHomeFragment();
                if (frag != null) frag.reloadAfterConfigChange();
                if (isMonitoring) stopMonitoring();
                if (sharedPrefs.getAutoStreamEnabled()) {
                    mainHandler.postDelayed(new Runnable() {
                        @Override public void run() { startMonitoring(); }
                    }, 500);
                }
            }
        }).show();
    }

    private void showResetDataDialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.reset_data_title))
            .setMessage(getString(R.string.reset_data_message))
            .setPositiveButton(getString(R.string.btn_reset), new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    HomeFragment frag = getHomeFragment();
                    if (frag != null) frag.reloadAfterConfigChange();
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    private void openFeedbackEmail() {
        String version = "";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}
        String subject = getString(R.string.feedback_email, version);
        String mailto  = "mailto:" + FEEDBACK_EMAIL + "?subject=" + Uri.encode(subject);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mailto)));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, FEEDBACK_EMAIL, Toast.LENGTH_LONG).show();
        }
    }

    private HomeFragment getHomeFragment() {
        return (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.homePanel);
    }
}
