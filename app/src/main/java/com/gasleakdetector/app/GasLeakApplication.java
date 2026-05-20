/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-05-17
 */
package com.gasleakdetector.app;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.gasleakdetector.data.api.FcmTokenApiService;
import com.gasleakdetector.data.model.HistoricalDataPoint;
import com.gasleakdetector.data.prefs.SharedPrefs;
import com.gasleakdetector.notification.NotificationChannelManager;
import com.gasleakdetector.util.CrashHandler;
import com.gasleakdetector.util.LocaleHelper;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.ArrayList;
import java.util.List;

public class GasLeakApplication extends Application {

    private boolean historicalDataLoaded = false;
    private List<HistoricalDataPoint> cachedDataPoints = new ArrayList<>();

    public boolean isHistoricalDataLoaded() { return historicalDataLoaded; }
    public void setHistoricalDataLoaded(boolean loaded) { this.historicalDataLoaded = loaded; }

    public List<HistoricalDataPoint> getCachedNodes() { return cachedDataPoints; }

    public void setCachedNodes(List<HistoricalDataPoint> points) {
        cachedDataPoints = points != null ? points : new ArrayList<HistoricalDataPoint>();
    }

    public boolean hasInMemoryData() { return !cachedDataPoints.isEmpty(); }

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.init(this);
        try {
            NotificationChannelManager.createChannels(this);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        initFcm();
    }

    /**
     * Initialises FCM: always enables auto-init and fetches the token so it is
     * persisted locally before the user finishes setup. Registration with the
     * backend happens here only when a config is already saved; otherwise
     * MainActivity.onConfigSaved() will do it immediately after the user saves
     * their server config for the first time.
     */
    private void initFcm() {
        final SharedPrefs prefs = new SharedPrefs(getApplicationContext());

        if (!prefs.getFcmPushEnabled()) {
            FirebaseMessaging.getInstance().setAutoInitEnabled(false);
            return;
        }

        /* Always enable auto-init when FCM push is on so the token is generated
         * even before the user has entered the server config. */
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);

        FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(token -> {
                if (token == null || token.isEmpty()) return;
                Log.d("GasLeakApplication", "FCM token obtained on startup");
                prefs.setFcmToken(token);

                /* Register with backend only when config is already available.
                 * First-run case is handled in MainActivity.onConfigSaved(). */
                if (prefs.hasRealtimeConfig()) {
                    FcmTokenApiService.register(
                        getApplicationContext(),
                        prefs.getRealtimeConfig(),
                        token,
                        null /* fire-and-forget */
                    );
                }
            })
            .addOnFailureListener(e ->
                Log.w("GasLeakApplication", "FCM getToken failed: " + e.getMessage())
            );
    }

    @Override
    protected void attachBaseContext(Context base) {
        /* Apply the saved locale before any Activity is created. */
        String lang = new SharedPrefs(base).getLanguage();
        super.attachBaseContext(LocaleHelper.applyLocale(base, lang));
    }
}
