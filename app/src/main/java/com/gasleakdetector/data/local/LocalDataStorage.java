/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-04-23
 */
package com.gasleakdetector.data.local;

import android.content.Context;
import android.util.Log;
import com.gasleakdetector.data.model.HistoricalDataPoint;
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
 * Persists gas data points to a local JSON file so the app
 * can show the last known readings when offline.
 */
public class LocalDataStorage {

    private static final String TAG       = "LocalDataStorage";
    private static final String FILE_NAME = "gas_nodes_cache.json";
    private static final int    MAX_NODES = 1000;

    private final File   cacheFile;
    private final Object writeLock = new Object(); // #8: serialize concurrent disk writes

    public LocalDataStorage(Context context) {
        this.cacheFile = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
    }

    /**
     * Writes data points to disk, capping at {@code MAX_NODES} to avoid unbounded growth.
     * Silently returns false on I/O error.
     */
    public boolean saveNodes(List<HistoricalDataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.isEmpty()) return false;

        try {
            List<HistoricalDataPoint> nodesToSave = dataPoints;
            if (dataPoints.size() > MAX_NODES) {
                nodesToSave = dataPoints.subList(dataPoints.size() - MAX_NODES, dataPoints.size());
            }

            JSONArray nodesArray = new JSONArray();
            for (HistoricalDataPoint point : nodesToSave) {
                JSONObject node = new JSONObject();
                node.put("id",         point.getId());
                node.put("device_id",  point.getDeviceId());
                node.put("gas_ppm",    point.getGasPpm());
                node.put("status",     point.getStatus());
                node.put("ip_address", point.getIpAddress());
                node.put("created_at", point.getCreatedAt());
                nodesArray.put(node);
            }

            JSONObject root = new JSONObject();
            root.put("saved_at", System.currentTimeMillis());
            root.put("count",    nodesToSave.size());
            root.put("nodes",    nodesArray);

            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(root.toString().getBytes(StandardCharsets.UTF_8));
            }
            return true;

        } catch (Exception e) {
            Log.w(TAG, "Failed to save cache", e);
            return false;
        }
    }

    /** Appends a single new point to the existing cache file. */
    public boolean addNode(HistoricalDataPoint newPoint) {
        if (newPoint == null) return false;
        synchronized (writeLock) { // #8: prevent concurrent read-modify-write data loss
            List<HistoricalDataPoint> current = loadNodes();
            current.add(newPoint);
            return saveNodes(current);
        }
    }

    /** Reads cached data points from disk. Returns an empty list on any error. */
    public List<HistoricalDataPoint> loadNodes() {
        List<HistoricalDataPoint> dataPoints = new ArrayList<>();
        if (!cacheFile.exists()) return dataPoints;

        try {
            try (FileInputStream fis = new FileInputStream(cacheFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONArray nodesArray = new JSONObject(sb.toString()).getJSONArray("nodes");
                for (int i = 0; i < nodesArray.length(); i++) {
                    JSONObject node = nodesArray.getJSONObject(i);
                    HistoricalDataPoint point = new HistoricalDataPoint();
                    point.setId(node.optLong("id")); // #12: use optLong — Supabase IDs are bigint
                    point.setDeviceId(node.optString("device_id"));
                    point.setGasPpm(node.optInt("gas_ppm"));
                    point.setStatus(node.optString("status"));
                    point.setIpAddress(node.optString("ip_address"));
                    point.setCreatedAt(node.optString("created_at"));
                    dataPoints.add(point);
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to parse cache file, returning empty list", e);
        }

        return dataPoints;
    }

    public boolean clearCache() {
        return !cacheFile.exists() || cacheFile.delete();
    }

    public boolean hasCache() {
        return cacheFile.exists() && cacheFile.length() > 0;
    }

    /** Returns a human-readable summary of what's in the cache. */
    public String getCacheInfo() {
        if (!hasCache()) return "No cache available";
        try {
            try (FileInputStream fis = new FileInputStream(cacheFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONObject root  = new JSONObject(sb.toString());
                long savedAt     = root.optLong("saved_at", 0);
                int  count       = root.optInt("count", 0);
                String savedDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                                       .format(new java.util.Date(savedAt));
                return String.format("Cached %d nodes (saved at: %s)", count, savedDate);
            }
        } catch (Exception e) {
            return "Cache info unavailable";
        }
    }
}
