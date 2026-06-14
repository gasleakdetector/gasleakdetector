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
package com.gasleakdetector.data.local;

import android.content.Context;
import android.util.Log;
import com.gasleakdetector.data.model.HourlyStatPoint;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists hourly stat points to a local JSON file so the Statistics
 * screen can show the last known data when offline or before the
 * network response arrives.
 */
public class StatsLocalStorage {

    private static final String TAG       = "StatsLocalStorage";
    private static final String FILE_NAME = "stats_cache.json";

    private final File   cacheFile;
    private final Object writeLock = new Object();

    public StatsLocalStorage(Context context) {
        this.cacheFile = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
    }

    /* ------------------------------------------------------------------ */
    /*  Write                                                               */
    /* ------------------------------------------------------------------ */

    /** Replaces all cached stat points with the given list. Thread-safe. */
    public boolean saveStats(List<HourlyStatPoint> points) {
        if (points == null || points.isEmpty()) return false;
        synchronized (writeLock) {
            try {
                JSONArray arr = new JSONArray();
                for (HourlyStatPoint p : points) {
                    JSONObject o = new JSONObject();
                    o.put("bucket",       p.getBucket());
                    o.put("avg_gas",      p.getAvgGas());
                    o.put("min_gas",      p.getMinGas());
                    o.put("max_gas",      p.getMaxGas());
                    o.put("sample_count", p.getSampleCount());
                    arr.put(o);
                }
                JSONObject root = new JSONObject();
                root.put("saved_at", System.currentTimeMillis());
                root.put("count",    points.size());
                root.put("points",   arr);

                try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                    fos.write(root.toString().getBytes(StandardCharsets.UTF_8));
                }
                Log.d(TAG, "saveStats: " + points.size() + " points saved");
                return true;
            } catch (Exception e) {
                Log.w(TAG, "Failed to save stats cache", e);
                return false;
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Read                                                                */
    /* ------------------------------------------------------------------ */

    /** Reads cached stat points from disk. Returns empty list on any error. */
    public List<HourlyStatPoint> loadStats() {
        List<HourlyStatPoint> result = new ArrayList<>();
        if (!cacheFile.exists()) return result;
        try {
            try (FileInputStream fis = new FileInputStream(cacheFile);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONArray arr = new JSONObject(sb.toString()).getJSONArray("points");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    HourlyStatPoint p = new HourlyStatPoint();
                    p.setBucket(o.optString("bucket", ""));
                    p.setAvgGas((float) o.optDouble("avg_gas", 0));
                    p.setMinGas((float) o.optDouble("min_gas", 0));
                    p.setMaxGas((float) o.optDouble("max_gas", 0));
                    p.setSampleCount(o.optInt("sample_count", 0));
                    result.add(p);
                }
                Log.d(TAG, "loadStats: " + result.size() + " points loaded");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse stats cache, returning empty list", e);
        }
        return result;
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                             */
    /* ------------------------------------------------------------------ */

    public boolean hasCache() {
        return cacheFile.exists() && cacheFile.length() > 0;
    }

    /** Returns save timestamp in millis, or 0 if no cache. */
    public long getSavedAt() {
        if (!hasCache()) return 0;
        try {
            try (FileInputStream fis = new FileInputStream(cacheFile);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return new JSONObject(sb.toString()).optLong("saved_at", 0);
            }
        } catch (Exception e) {
            return 0;
        }
    }
}
