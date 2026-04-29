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
package com.gasleakdetector.service;

import android.util.Log;
import com.gasleakdetector.R;
import com.gasleakdetector.data.api.FcmTokenApiService;
import com.gasleakdetector.data.model.GasStatus;
import com.gasleakdetector.data.prefs.SharedPrefs;
import com.gasleakdetector.notification.GasNotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * Receives FCM push messages delivered by the Vercel backend.
 *
 * Expected data payload keys (set by /api/fcm/send on the server):
 *   gas_ppm   — integer concentration as string, e.g. "850"
 *   status    — "normal" | "warning" | "danger"
 *   device_id — originating ESP device identifier
 *   timestamp — ISO-8601 string (informational only)
 *
 * Token lifecycle: onNewToken fires on first install and whenever FCM
 * rotates the registration token. Both cases re-register with the backend
 * so the server always has a valid delivery address for this device.
 */
public class FcmService extends FirebaseMessagingService {

    private static final String TAG = "FcmService";

    /* Data payload key names — must match the server-side /api/fcm/send implementation. */
    private static final String KEY_GAS_PPM   = "gas_ppm";
    private static final String KEY_DEVICE_ID = "device_id";

    /* ------------------------------------------------------------------ */
    /* Token lifecycle                                                       */
    /* ------------------------------------------------------------------ */

    /**
     * Called by FCM when a new registration token is generated.
     * Persists the token and triggers re-registration with the backend.
     */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed");

        SharedPrefs prefs = new SharedPrefs(getApplicationContext());
        prefs.setFcmToken(token);

        /* Only register when the user has FCM push enabled and a config is present. */
        if (prefs.getFcmPushEnabled() && prefs.hasRealtimeConfig()) {
            FcmTokenApiService.register(
                getApplicationContext(),
                prefs.getRealtimeConfig(),
                token,
                null /* fire-and-forget: no UI to update here */
            );
        }
    }

    /* ------------------------------------------------------------------ */
    /* Message handling                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Called when a FCM data message arrives.
     * High-priority FCM data messages wake the device and deliver here
     * even when the app process is killed — this is the key advantage over
     * the existing foreground-only notification path.
     */
    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        SharedPrefs prefs = new SharedPrefs(getApplicationContext());

        /* Respect both the FCM push toggle and the master notification toggle. */
        if (!prefs.getFcmPushEnabled() || !prefs.getNotificationsEnabled()) {
            Log.d(TAG, "FCM push suppressed by user preference");
            return;
        }

        Map<String, String> data = message.getData();
        if (data == null || data.isEmpty()) {
            Log.w(TAG, "Received FCM message with no data payload — ignoring");
            return;
        }

        int ppm   = parsePpm(data.get(KEY_GAS_PPM));
        int level = GasStatus.calculateLevel(ppm);

        /* Skip normal readings — do not spam the notification shade. */
        if (level == GasStatus.LEVEL_NORMAL) {
            Log.d(TAG, "FCM: normal reading (" + ppm + " ppm) — no notification");
            return;
        }

        String    device = data.containsKey(KEY_DEVICE_ID) ? data.get(KEY_DEVICE_ID) : "";
        GasStatus status = buildStatus(ppm, level, device);
        new GasNotificationHelper(getApplicationContext()).showAlert(status);
        Log.d(TAG, "FCM alert posted: " + ppm + " ppm, level=" + level);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                               */
    /* ------------------------------------------------------------------ */

    /** Parses the raw ppm string; returns 0 on parse failure instead of crashing. */
    private int parsePpm(String raw) {
        if (raw == null || raw.isEmpty()) return 0;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Cannot parse gas_ppm value: '" + raw + "'");
            return 0;
        }
    }

    /** Builds a GasStatus from the FCM payload for use in a local notification. */
    private GasStatus buildStatus(int ppm, int level, String deviceId) {
        String prefix  = (deviceId != null && !deviceId.isEmpty()) ? "[" + deviceId + "] " : "";
        String message = (level == GasStatus.LEVEL_DANGER)
            ? prefix + getString(R.string.msg_danger,  ppm)
            : prefix + getString(R.string.msg_warning, ppm);
        return new GasStatus(level, ppm, System.currentTimeMillis(), message);
    }
}
