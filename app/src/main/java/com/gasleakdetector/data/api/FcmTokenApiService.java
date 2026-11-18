/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-04-29
 */
package com.gasleakdetector.data.api;

import android.content.Context;
import android.util.Log;
import com.gasleakdetector.data.model.RealtimeConfig;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Registers or unregisters an FCM device token with the Vercel backend.
 *
 * Backend endpoint contract (POST /api/fcm/register):
 *   Headers : x-api-key: <apiKey>
 *   Body    : { "token": "<fcmToken>", "device_id": "<deviceId>" }
 *   200 OK  : { "ok": true }
 *   4xx/5xx : { "error": "<message>" }
 *
 * This call is fire-and-forget by design — failures are logged but not
 * surfaced to the user, because FCM will simply re-trigger onNewToken
 * the next time a message cannot be delivered, giving the app another
 * opportunity to register.
 */
public class FcmTokenApiService {

    private static final String TAG      = "FcmTokenApiService";
    private static final String ENDPOINT = "/api/fcm/register";
    private static final int    TIMEOUT  = 15_000;

    public interface RegisterCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Sends the FCM token to the backend on a new background thread.
     * {@code callback} may be null for fire-and-forget usage.
     */
    public static void register(
            final Context context,
            final RealtimeConfig config,
            final String fcmToken,
            final RegisterCallback callback) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    /* Build JSON body. */
                    JSONObject body = new JSONObject();
                    body.put("token", fcmToken);
                    if (config.getDeviceId() != null && !config.getDeviceId().trim().isEmpty()) {
                        body.put("device_id", config.getDeviceId().trim());
                    }
                    byte[] bodyBytes = body.toString().getBytes("UTF-8");

                    URL url = new URL(config.getApiUrl() + ENDPOINT);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("x-api-key", config.getApiKey());
                    conn.setConnectTimeout(TIMEOUT);
                    conn.setReadTimeout(TIMEOUT);
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    os.write(bodyBytes);
                    os.flush();
                    os.close();

                    int code = conn.getResponseCode();
                    if (code == 200 || code == 201) {
                        Log.d(TAG, "FCM token registered successfully");
                        if (callback != null) callback.onSuccess();
                    } else {
                        String err = "HTTP " + code;
                        Log.w(TAG, "FCM register failed: " + err);
                        if (callback != null) callback.onError(err);
                    }

                } catch (final Exception e) {
                    Log.e(TAG, "FCM register error", e);
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
