/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/traitimtrongvag/gasleakdetector-app
 * Modified: 2026-03-15
 */

package com.gasleak.data.api;

import android.util.Log;
import com.gasleak.data.model.RealtimeConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LogsApiService {

    private static final String TAG   = "LogsApiService";
    private static final int    LIMIT = 100;

    public interface LogsCallback {
        void onSuccess(List<JSONObject> logs, Long nextCursor);
        void onError(String error);
    }

    public static void fetchLogs(
            final RealtimeConfig config,
            final Long cursor,
            final LogsCallback callback) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append(config.getApiUrl()).append("/api/logs");
                    sb.append("?limit=").append(LIMIT);
                    if (config.getDeviceId() != null && !config.getDeviceId().trim().isEmpty()) {
                        sb.append("&device_id=").append(config.getDeviceId().trim());
                    }
                    if (cursor != null) sb.append("&cursor=").append(cursor);

                    Log.d(TAG, "Fetching logs: " + sb);
                    URL url = new URL(sb.toString());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("x-api-key", config.getApiKey());
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    int code = conn.getResponseCode();
                    if (code != 200) throw new Exception("HTTP " + code);

                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"), 8192);
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                    reader.close();

                    JSONObject root    = new JSONObject(body.toString());
                    JSONArray arr      = root.optJSONArray("logs");
                    final Long next    = root.isNull("nextCursor") ? null : root.optLong("nextCursor");

                    final List<JSONObject> logs = new ArrayList<JSONObject>();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            logs.add(arr.getJSONObject(i));
                        }
                    }

                    Log.d(TAG, "Got " + logs.size() + " logs, nextCursor=" + next);
                    if (callback != null) callback.onSuccess(logs, next);

                } catch (final Exception e) {
                    Log.e(TAG, "fetchLogs error", e);
                    if (callback != null) {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error");
                    }
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }
}