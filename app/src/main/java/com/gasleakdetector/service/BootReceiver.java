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
package com.gasleakdetector.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.gasleakdetector.data.api.FcmTokenApiService;
import com.gasleakdetector.data.prefs.SharedPrefs;
import com.google.firebase.messaging.FirebaseMessaging;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
            return;
        }

        SharedPrefs prefs = new SharedPrefs(context.getApplicationContext());
        if (!prefs.getFcmPushEnabled() || !prefs.hasRealtimeConfig()) return;

        FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(token -> {
                if (token == null || token.isEmpty()) return;
                prefs.setFcmToken(token);
                FcmTokenApiService.register(
                    context.getApplicationContext(),
                    prefs.getRealtimeConfig(),
                    token,
                    null
                );
                Log.d(TAG, "FCM token re-registered after boot");
            })
            .addOnFailureListener(e ->
                Log.w(TAG, "FCM getToken after boot failed: " + e.getMessage())
            );
    }
}
