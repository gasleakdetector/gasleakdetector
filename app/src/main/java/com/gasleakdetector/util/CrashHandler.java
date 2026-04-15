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
package com.gasleakdetector.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Catches uncaught exceptions, writes a crash report to internal storage,
 * then delegates to the default handler so Android can show its crash dialog.
 *
 * Why internal storage: external storage requires MANAGE_EXTERNAL_STORAGE
 * on API 30+. Internal filesDir is always writable without extra permissions.
 *
 * Why delegate to DEFAULT_HANDLER: not delegating leaves the main thread
 * blocked inside the handler — Toast won't dispatch, app appears frozen,
 * and Android eventually kills the process silently.
 */
public final class CrashHandler {

    private static final String TAG = "CrashHandler";

    public static void init(final Context app) {
        final Thread.UncaughtExceptionHandler defaultHandler =
            Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    writeCrashLog(app, throwable);
                } catch (Throwable ignored) {
                    Log.e(TAG, "Failed to write crash log", ignored);
                } finally {
                    // Always delegate — this unblocks the main thread and lets
                    // Android display its standard crash dialog.
                    if (defaultHandler != null) {
                        defaultHandler.uncaughtException(thread, throwable);
                    } else {
                        System.exit(1);
                    }
                }
            }
        });
    }

    private static void writeCrashLog(Context app, Throwable throwable) throws Exception {
        String time = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.US).format(new Date());

        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));

        String versionName = "unknown";
        long   versionCode = 0;
        try {
            PackageInfo info = app.getPackageManager()
                .getPackageInfo(app.getPackageName(), 0);
            versionName = info.versionName;
            versionCode = Build.VERSION.SDK_INT >= 28
                ? info.getLongVersionCode()
                : info.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {}

        String report = "============= Crash Report =============\n"
            + "Time               : " + time + "\n"
            + "Device             : " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
            + "Android            : " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n"
            + "App version        : " + versionName + " (" + versionCode + ")\n"
            + "========================================\n\n"
            + sw;

        // Write to internal storage — always writable, no permissions needed
        File dir = new File(app.getFilesDir(), "crashes");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "crash_" + time + ".txt");

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(report.getBytes("UTF-8"));
        fos.close();

        Log.e(TAG, "Crash log: " + file.getAbsolutePath());
        Log.e(TAG, report);
    }
}
