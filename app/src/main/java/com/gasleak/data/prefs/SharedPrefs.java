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
package com.gasleak.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import com.gasleak.data.model.RealtimeConfig;

public class SharedPrefs {

    private static final String PREFS_NAME               = "GasLeakPrefs";
    private static final String KEY_THEME                = "theme";
    private static final String KEY_API_URL              = "api_url";
    private static final String KEY_API_KEY              = "api_key";
    private static final String KEY_DEVICE_ID            = "device_id";
    private static final String KEY_NOTIFICATIONS        = "notifications_enabled";
    private static final String KEY_AUTO_REFRESH         = "auto_refresh_enabled";
    private static final String KEY_AUTO_STREAM          = "auto_stream_enabled";
    private static final String KEY_KEEP_RUNNING         = "keep_app_running";
    private static final String KEY_LANGUAGE             = "language";
    private static final String KEY_LAST_FETCH_TIME      = "last_fetch_time";
    private static final String KEY_INTRO_SHOWN          = "intro_shown";

    /* Re-fetch historical data if the last fetch is older than this. */
    private static final long REFETCH_INTERVAL_MS = 5 * 60 * 1000L;

    private static final String[] SUPPORTED_LANGUAGES = {"en", "vi", "zh", "ja", "ko", "fr", "es", "de"};

    private final SharedPreferences prefs;

    public SharedPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setTheme(int theme) { prefs.edit().putInt(KEY_THEME, theme).apply(); }
    public int  getTheme()          { return prefs.getInt(KEY_THEME, 1); }

    public void saveRealtimeConfig(RealtimeConfig config) {
        prefs.edit()
            .putString(KEY_API_URL,   config.getApiUrl())
            .putString(KEY_API_KEY,   config.getApiKey())
            .putString(KEY_DEVICE_ID, config.getDeviceId())
            .apply();
    }

    public RealtimeConfig getRealtimeConfig() {
        String apiUrl = prefs.getString(KEY_API_URL, "");
        String apiKey = prefs.getString(KEY_API_KEY, "");
        if (apiUrl.isEmpty() || apiKey.isEmpty()) return null;
        return new RealtimeConfig(apiUrl, apiKey, prefs.getString(KEY_DEVICE_ID, ""));
    }

    public boolean hasRealtimeConfig() { return getRealtimeConfig() != null; }

    public void    setNotificationsEnabled(boolean v) { prefs.edit().putBoolean(KEY_NOTIFICATIONS, v).apply(); }
    public boolean getNotificationsEnabled()           { return prefs.getBoolean(KEY_NOTIFICATIONS, true); }

    public void    setAutoRefreshEnabled(boolean v) { prefs.edit().putBoolean(KEY_AUTO_REFRESH, v).apply(); }
    public boolean getAutoRefreshEnabled()           { return prefs.getBoolean(KEY_AUTO_REFRESH, true); }

    public void    setAutoStreamEnabled(boolean v) { prefs.edit().putBoolean(KEY_AUTO_STREAM, v).apply(); }
    public boolean getAutoStreamEnabled()           { return prefs.getBoolean(KEY_AUTO_STREAM, true); }

    public void    setKeepAppRunning(boolean v) { prefs.edit().putBoolean(KEY_KEEP_RUNNING, v).apply(); }
    public boolean getKeepAppRunning()           { return prefs.getBoolean(KEY_KEEP_RUNNING, true); }

    public void   setLanguage(String langCode) { prefs.edit().putString(KEY_LANGUAGE, langCode).apply(); }

    public String getLanguage() {
        String systemLang = java.util.Locale.getDefault().getLanguage();
        String defaultLang = "en";
        for (String lang : SUPPORTED_LANGUAGES) {
            if (lang.equals(systemLang)) { defaultLang = systemLang; break; }
        }
        return prefs.getString(KEY_LANGUAGE, defaultLang);
    }

    public void    markFetchTime()     { prefs.edit().putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis()).apply(); }
    public void    clearFetchTime()    { prefs.edit().remove(KEY_LAST_FETCH_TIME).apply(); }

    public boolean shouldRefetchData() {
        long last = prefs.getLong(KEY_LAST_FETCH_TIME, 0);
        return (System.currentTimeMillis() - last) > REFETCH_INTERVAL_MS;
    }

    public boolean isIntroShown()       { return prefs.getBoolean(KEY_INTRO_SHOWN, false); }
    public void    setIntroShown(boolean v) { prefs.edit().putBoolean(KEY_INTRO_SHOWN, v).apply(); }

    public void resetToDefaults() {
        prefs.edit()
            .putInt(KEY_THEME, 1)
            .putBoolean(KEY_NOTIFICATIONS, true)
            .putBoolean(KEY_AUTO_REFRESH, true)
            .putBoolean(KEY_AUTO_STREAM, true)
            .putBoolean(KEY_KEEP_RUNNING, true)
            .apply();
    }
}
