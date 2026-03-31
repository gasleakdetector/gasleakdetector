/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-03-30
 */
package com.gasleak.data.api;

import android.util.Log;
import com.gasleak.data.model.HistoricalDataPoint;
import com.gasleak.data.model.RealtimeConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Fetches historical gas readings from the backend REST API.
 *
 * Handles cursor-based pagination transparently — callers receive the full
 * result set via the callback. Supports optional gzip decompression.
 */
public class HistoricalApiService {

    private static final String TAG     = "HistoricalApiService";
    private static final int    TIMEOUT = 20_000;

    public interface HistoricalDataCallback {
        void onSuccess(List<HistoricalDataPoint> dataPoints);
        void onError(String error);
    }

    public interface HistoricalDataRangeCallback {
        void onProgress(int loaded); // called after each page; useful for progress indicators
        void onSuccess(List<HistoricalDataPoint> dataPoints, String range);
        void onError(String error);
    }

    // ── Called from MainActivity with a default 1d range ─────────────────────

    public static void fetchHistoricalData(
            final RealtimeConfig config,
            final HistoricalDataCallback callback) {

        fetchAllPages(config, "1d", new HistoricalDataRangeCallback() {
            @Override public void onProgress(int loaded) {}
            @Override public void onSuccess(List<HistoricalDataPoint> pts, String range) {
                if (callback != null) callback.onSuccess(pts);
            }
            @Override public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    // ── Called from StatisticsFragment with an explicit range and progress callback ──

    public static void fetchHistoricalData(
            final RealtimeConfig config,
            final String range,
            final HistoricalDataRangeCallback callback) {
        fetchAllPages(config, range, callback);
    }

    // ── Fetches all pages until nextCursor is null ────────────────────────────

    private static void fetchAllPages(
            final RealtimeConfig config,
            final String range,
            final HistoricalDataRangeCallback callback) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<HistoricalDataPoint> allPoints = new ArrayList<>();
                Long cursor = null;

                try {
                    while (true) {
                        PageResult page = fetchOnePage(config, range, cursor);
                        allPoints.addAll(page.points);

                        // Report progress after each page so the UI can show a count
                        if (callback != null) callback.onProgress(allPoints.size());

                        Log.d(TAG, "Page loaded: " + page.points.size()
                            + " | total=" + allPoints.size()
                            + " | nextCursor=" + page.nextCursor);

                        if (page.nextCursor == null) break; // No more pages
                        cursor = page.nextCursor;
                    }

                    Log.d(TAG, "All done: " + allPoints.size() + " nodes for range=" + range);
                    if (callback != null) callback.onSuccess(allPoints, range);

                } catch (final Exception e) {
                    Log.e(TAG, "fetchAllPages error", e);
                    if (callback != null) {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error");
                    }
                }
            }
        }).start();
    }

    private static PageResult fetchOnePage(
            RealtimeConfig config,
            String range,
            Long cursor) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append(config.getApiUrl()).append("/api/historical");
        sb.append("?range=").append(range != null ? range : "1d");
        if (config.getDeviceId() != null && !config.getDeviceId().trim().isEmpty()) {
            sb.append("&device_id=").append(config.getDeviceId().trim());
        }
        if (cursor != null) sb.append("&cursor=").append(cursor);

        Log.d(TAG, "GET " + sb);
        URL url = new URL(sb.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("x-api-key", config.getApiKey());
        conn.setRequestProperty("Accept-Encoding", "gzip");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);

        int code = conn.getResponseCode();
        if (code != 200) { conn.disconnect(); throw new Exception("HTTP " + code); }

        String body = readResponse(conn);
        conn.disconnect();

        JSONObject root     = new JSONObject(body);
        JSONArray  arr      = root.optJSONArray("data");
        Long       nextCursor = root.isNull("nextCursor") ? null : root.optLong("nextCursor");

        List<HistoricalDataPoint> points = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                HistoricalDataPoint pt = new HistoricalDataPoint();
                pt.setId(o.optLong("id", 0));
                pt.setGasPpm(o.optInt("gas_ppm", 0));
                // #11: read correct aggregate fields — were all reading gas_ppm before
                pt.setAvgGas((float) o.optDouble("avg_gas", o.optInt("gas_ppm", 0)));
                pt.setMinGas((float) o.optDouble("min_gas", o.optInt("gas_ppm", 0)));
                pt.setMaxGas((float) o.optDouble("max_gas", o.optInt("gas_ppm", 0)));
                pt.setSampleCount(1);
                pt.setStatus(o.optString("status", "normal"));
                pt.setDeviceId(o.optString("device_id", ""));
                pt.setCreatedAt(o.optString("created_at", ""));
                pt.setBucket(o.optString("created_at", ""));
                points.add(pt);
            }
        }

        return new PageResult(points, nextCursor);
    }

    // ── Reads the response body, inflating gzip if the server compressed it ──

    private static String readResponse(HttpURLConnection conn) throws IOException {
        String encoding = conn.getContentEncoding();
        InputStream raw = new BufferedInputStream(conn.getInputStream());

        InputStream stream = (encoding != null && encoding.equalsIgnoreCase("gzip"))
            ? new GZIPInputStream(raw)
            : raw;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = stream.read(chunk)) != -1) buffer.write(chunk, 0, n);
        stream.close();
        return buffer.toString("UTF-8");
    }

    // ── Internal result holder for a single page fetch ────────────────────────

    private static class PageResult {
        final List<HistoricalDataPoint> points;
        final Long nextCursor;

        PageResult(List<HistoricalDataPoint> points, Long nextCursor) {
            this.points     = points;
            this.nextCursor = nextCursor;
        }
    }
}
