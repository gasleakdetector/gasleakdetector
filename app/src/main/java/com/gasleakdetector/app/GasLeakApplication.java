/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-04-15
 */
package com.gasleakdetector.app;

import android.app.Application;
import android.content.Context;
import com.gasleakdetector.data.model.HistoricalDataPoint;
import com.gasleakdetector.data.prefs.SharedPrefs;
import com.gasleakdetector.notification.NotificationChannelManager;
import com.gasleakdetector.util.CrashHandler;
import com.gasleakdetector.util.LocaleHelper;
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
    }

    @Override
    protected void attachBaseContext(Context base) {
        /* Apply the saved locale before any Activity is created. */
        String lang = new SharedPrefs(base).getLanguage();
        super.attachBaseContext(LocaleHelper.applyLocale(base, lang));
    }
}
