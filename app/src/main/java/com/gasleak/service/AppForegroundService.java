/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-03-14
 */
package com.gasleak.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.gasleak.R;
import com.gasleak.notification.NotificationChannelManager;
import com.gasleak.ui.main.MainActivity;

/**
 * Keeps the app alive in the background so the WebSocket connection
 * isn't destroyed by the OS when the user switches away.
 */
public class AppForegroundService extends Service {

    public static final int    NOTIF_ID     = 1001;
    public static final String ACTION_EXIT  = "com.gasleak.ACTION_EXIT";
    public static final String ACTION_START = "com.gasleak.ACTION_START";
    public static final String ACTION_STOP  = "com.gasleak.ACTION_STOP";

    public static void start(Context ctx) {
        Intent i = new Intent(ctx, AppForegroundService.class);
        i.setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }

    public static void stop(Context ctx) {
        Intent i = new Intent(ctx, AppForegroundService.class);
        i.setAction(ACTION_STOP);
        ctx.startService(i);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_EXIT.equals(action)) {
            stopForeground(true);
            stopSelf();
            android.os.Process.killProcess(android.os.Process.myPid());
            return START_NOT_STICKY;
        }

        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        NotificationChannelManager.createChannels(this);
        startForeground(NOTIF_ID, buildNotification());
        return START_STICKY;
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent openPi = PendingIntent.getActivity(this, 0, openIntent, piFlags);

        Intent exitIntent = new Intent(this, AppForegroundService.class);
        exitIntent.setAction(ACTION_EXIT);
        PendingIntent exitPi = PendingIntent.getService(this, 1, exitIntent, piFlags);

        return new NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_FOREGROUND)
            .setContentTitle(getString(R.string.notif_foreground_title))
            .setContentText(getString(R.string.notif_foreground_text))
            .setSmallIcon(R.drawable.ic_fire)
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_stop, getString(R.string.notif_action_exit), exitPi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
