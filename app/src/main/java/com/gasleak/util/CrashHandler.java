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
package com.gasleak.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Catches uncaught exceptions, writes a crash report to disk,
 * and shows a toast before the process exits.
 */
public final class CrashHandler {

    public static final Thread.UncaughtExceptionHandler DEFAULT_HANDLER =
        Thread.getDefaultUncaughtExceptionHandler();

    public static void init(final Context app) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e("AppCrash", "Uncaught exception on thread: " + thread.getName());
                try {
                    handleCrash(thread, throwable);
                } catch (Throwable e) {
                    e.printStackTrace();
                    if (DEFAULT_HANDLER != null) DEFAULT_HANDLER.uncaughtException(thread, throwable);
                    else System.exit(2);
                }
            }

            private void handleCrash(Thread thread, Throwable throwable) throws InterruptedException {
                Log.e("AppCrash", "Writing crash log");

                final String time     = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss").format(new Date());
                String       fileName = "crash_log_" + time + ".txt";
                String       dirName;

                if (Build.VERSION.SDK_INT >= 30) {
                    dirName = "/storage/emulated/0/Documents/";
                } else {
                    dirName = String.valueOf(app.getExternalFilesDir(null));
                }

                File crashFile = new File(dirName, fileName);

                String versionName = "unknown";
                long   versionCode = 0;
                try {
                    PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                    versionName = info.versionName;
                    versionCode = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
                } catch (PackageManager.NameNotFoundException ignored) {}

                StringWriter sw = new StringWriter();
                throwable.printStackTrace(new PrintWriter(sw));
                String fullStackTrace = sw.toString();

                StringBuilder report = new StringBuilder();
                report.append("============= Crash Report =============\n");
                report.append("Time Of Crash      : ").append(time).append("\n");
                report.append("Device Manufacturer: ").append(Build.MANUFACTURER).append("\n");
                report.append("Device Model       : ").append(Build.MODEL).append("\n");
                report.append("Android Version    : ").append(Build.VERSION.RELEASE).append("\n");
                report.append("Android SDK        : ").append(Build.VERSION.SDK_INT).append("\n");
                report.append("App VersionName    : ").append(versionName).append("\n");
                report.append("App VersionCode    : ").append(versionCode).append("\n");
                report.append("========================================\n\n");
                report.append(fullStackTrace);

                try {
                    writeFile(crashFile, report.toString());
                } catch (IOException ignored) {}

                Toast.makeText(app, "App crashed unexpectedly. Log saved.", Toast.LENGTH_LONG).show();
                Log.e("AppCrash", "Crash log written to: " + crashFile.getAbsolutePath());
            }

            private void writeFile(File file, String content) throws IOException {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(content.getBytes());
                try { fos.close(); } catch (IOException ignored) {}
            }
        });
    }
}
