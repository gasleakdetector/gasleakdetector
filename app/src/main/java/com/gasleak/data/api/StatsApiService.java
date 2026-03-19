/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-03-20
 */
package com.gasleak.data.api;

import android.util.Log;
import com.gasleak.data.model.HourlyStatPoint;
import com.gasleak.data.model.RealtimeConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StatsApiService {

    private static final String TAG     = "StatsApiService";
    private static final int    TIMEOUT = 15_000;
    private static final int    LIMIT   = 10;

    public interface StatsCallback {
        void onSuccess(List<HourlyStatPoint> points);
        void onError(String error);
    }

    public static void fetchHourlyStats(final RealtimeConfig config, final StatsCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append(config.getApiUrl()).append("/api/stats");
                    sb.append("?limit=").append(LIMIT);
                    if (config.getDeviceId() != null && !config.getDeviceId().trim().isEmpty()) {
                        sb.append("&device_id=").append(config.getDeviceId().trim());
                    }

                    URL url = new URL(sb.toString());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("x-api-key", config.getApiKey());
                    conn.setConnectTimeout(TIMEOUT);
                    conn.setReadTimeout(TIMEOUT);

                    int code = conn.getResponseCode();
                    if (code != 200) throw new Exception("HTTP " + code);

                    String body = readResponse(conn);
                    JSONObject root = new JSONObject(body);
                    JSONArray arr = root.optJSONArray("data");

                    List<HourlyStatPoint> points = new ArrayList<>();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            HourlyStatPoint p = new HourlyStatPoint();
                            p.setBucket(o.optString("bucket", ""));
                            p.setAvgGas((float) o.optDouble("avg_gas", 0));
                            p.setMinGas((float) o.optDouble("min_gas", 0));
                            p.setMaxGas((float) o.optDouble("max_gas", 0));
                            p.setSampleCount(o.optInt("sample_count", 0));
                            points.add(p);
                        }
                    }

                    Log.d(TAG, "fetchHourlyStats: " + points.size() + " points");
                    if (callback != null) callback.onSuccess(points);

                } catch (Exception e) {
                    Log.e(TAG, "fetchHourlyStats error", e);
                    if (callback != null) callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error");
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        InputStream stream = new BufferedInputStream(conn.getInputStream());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = stream.read(chunk)) != -1) buffer.write(chunk, 0, n);
        stream.close();
        return buffer.toString("UTF-8");
    }
}
