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
package com.gasleakdetector.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import com.gasleakdetector.R;

/** Creates and registers all notification channels required by the app. */
public class NotificationChannelManager {

    public static final String CHANNEL_FOREGROUND = "foreground_service";
    public static final String CHANNEL_GAS_ALERT  = "gas_alert";
    public static final String CHANNEL_STATUS      = "gas_status";
    public static final String CHANNEL_FCM_PUSH    = "fcm_push";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                createForegroundChannel(context, manager);
                createAlertChannel(context, manager);
                createStatusChannel(context, manager);
                createFcmPushChannel(context, manager);
            }
        }
    }

    private static void createForegroundChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_FOREGROUND,
                context.getString(R.string.notif_channel_foreground_name),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(context.getString(R.string.notif_channel_foreground_desc));
            channel.setShowBadge(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);
        }
    }

    private static void createAlertChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_GAS_ALERT,
                context.getString(R.string.notif_channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notif_channel_alert_desc));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            manager.createNotificationChannel(channel);
        }
    }

    private static void createStatusChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_STATUS,
                context.getString(R.string.notif_channel_status_name),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(context.getString(R.string.notif_channel_status_desc));
            channel.enableVibration(false);
            manager.createNotificationChannel(channel);
        }
    }

    private static void createFcmPushChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_FCM_PUSH,
                context.getString(R.string.notif_channel_fcm_push_name),
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notif_channel_fcm_push_desc));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            manager.createNotificationChannel(channel);
        }
    }
}
