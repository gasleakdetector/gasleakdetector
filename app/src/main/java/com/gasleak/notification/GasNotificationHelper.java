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
package com.gasleak.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.gasleak.R;
import com.gasleak.data.model.GasStatus;
import com.gasleak.ui.main.MainActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/** Builds and posts gas alert notifications. */
public class GasNotificationHelper {

    private static final int    NOTIFICATION_ID_STATUS = 1002;
    private static final String NOTIF_DATE_FORMAT      = "dd MMM yyyy, HH:mm:ss";

    /* Each alert gets a unique ID so they stack in the shade instead of replacing each other. */
    private static final AtomicInteger nextAlertId = new AtomicInteger(2000);

    private final Context             context;
    private final NotificationManager notificationManager;

    public GasNotificationHelper(Context context) {
        this.context             = context.getApplicationContext();
        this.notificationManager =
            (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void showAlert(GasStatus status) {
        if (status.isDanger())       showDangerNotification(status);
        else if (status.isWarning()) showWarningNotification(status);
    }

    private void showDangerNotification(GasStatus status) {
        notificationManager.notify(nextAlertId.getAndIncrement(), buildNotification(
            NotificationChannelManager.CHANNEL_GAS_ALERT,
            context.getString(R.string.notif_danger_title),
            status.getMessage() + "\n" + getCurrentDateTime(),
            Notification.PRIORITY_MAX
        ));
    }

    private void showWarningNotification(GasStatus status) {
        notificationManager.notify(nextAlertId.getAndIncrement(), buildNotification(
            NotificationChannelManager.CHANNEL_GAS_ALERT,
            context.getString(R.string.notif_warning_title),
            status.getMessage() + "\n" + getCurrentDateTime(),
            Notification.PRIORITY_HIGH
        ));
    }

    public void showStatusUpdate(GasStatus status) {
        notificationManager.notify(NOTIFICATION_ID_STATUS, buildNotification(
            NotificationChannelManager.CHANNEL_STATUS,
            context.getString(R.string.notif_status_title),
            status.getMessage(),
            Notification.PRIORITY_LOW
        ));
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat(NOTIF_DATE_FORMAT, Locale.getDefault()).format(new Date());
    }

    private Notification buildNotification(String channelId, String title, String message, int priority) {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, channelId);
        } else {
            builder = new Notification.Builder(context);
            builder.setPriority(priority);
        }
        return builder
            .setSmallIcon(R.drawable.ic_fire)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(createPendingIntent())
            .build();
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context, 0, intent, flags);
    }

    public void cancelAll() { notificationManager.cancelAll(); }
}
