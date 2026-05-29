/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Modified: 2026-05-29
 */
package com.gasleakdetector.util;

import android.content.Context;
import android.content.Intent;
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
 * Catches uncaught exceptions, writes the crash report to a temp file in
 * internal cache, then launches {@link CrashReportActivity} via a
 * FLAG_ACTIVITY_NEW_TASK intent so the user can read, copy, or open a
 * GitHub issue before the process exits.
 *
 * Why a separate Activity instead of AlertDialog:
 * At the point uncaughtException() is called the main thread's Looper is
 * either dead or about to be killed. Posting a dialog via Handler.post()
 * onto a dying Looper is not reliable -- the dialog may never appear or
 * the app may freeze. Starting a fresh Activity with NEW_TASK bypasses
 * the broken Looper entirely and gives us a clean window to work in.
 *
 * Why write to a file instead of passing the report through the Intent:
 * Large crash reports (deep stack traces, many causes) can exceed the
 * 1 MB Binder transaction limit and trigger TransactionTooLargeException.
 * Writing to getCacheDir() avoids that, requires no permissions, and the
 * file is cleaned up by CrashReportActivity after it is read.
 *
 * Why delegate to the default handler after starting the Activity:
 * Skipping delegation suppresses the system's own crash bookkeeping,
 * leaves the process in an undefined state, and prevents Android from
 * showing the "App stopped" dialog if our Activity fails to start.
 */
public final class CrashHandler {

    private static final String TAG             = "CrashHandler";
    static final         String REPORT_FILE_KEY = "crash_report_path";
    static final         String REPORT_FILE_NAME = "crash_report_pending.txt";

    public static void init(final Context app) {
        final Thread.UncaughtExceptionHandler defaultHandler =
            Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    String report = buildReport(app, throwable);
                    Log.e(TAG, report);

                    // Guard against a crash loop: if CrashReportActivity itself
                    // crashed (e.g. BadTokenException during resume), launching it
                    // again would crash infinitely.
                    if (!isCrashFromReportActivity(throwable)) {
                        writeReportToCache(app, report);
                        launchReportActivity(app);

                        // startActivity() only enqueues an intent -- it returns
                        // immediately while the Activity is still being created.
                        // Delegating to defaultHandler right away kills the process
                        // before CrashReportActivity gets a chance to render.
                        // Sleeping here gives the system enough time to bring the
                        // Activity to the foreground before we exit.
                        Thread.sleep(3000);
                    }
                } catch (Throwable secondary) {
                    Log.e(TAG, "CrashHandler failed", secondary);
                } finally {
                    // Let Android clean up the process normally.
                    if (defaultHandler != null) {
                        defaultHandler.uncaughtException(thread, throwable);
                    } else {
                        System.exit(1);
                    }
                }
            }
        });
    }

    // --- Report builder ---

    private static String buildReport(Context app, Throwable throwable) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US)
            .format(new Date());

        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString().trim();

        String pkgName     = app.getPackageName();
        String versionName = "unknown";
        long   versionCode = 0;

        try {
            PackageInfo info = app.getPackageManager()
                .getPackageInfo(pkgName, 0);
            versionName = info.versionName;
            versionCode = Build.VERSION.SDK_INT >= 28
                ? info.getLongVersionCode()
                : (long) info.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {}

        StringBuilder sb = new StringBuilder();

        sb.append("## Report Info\n\n\n");
        sb.append("**User Action**: `app crash`  \n");
        sb.append("**Sender**: `CrashHandler`  \n");
        sb.append("**Report Timestamp**: `").append(timestamp).append("`  \n");
        sb.append("##\n\n\n");

        sb.append("## App Crash\n\n");
        sb.append("```\n");
        sb.append(stackTrace).append("\n");
        sb.append("```\n\n\n\n");

        sb.append("## App Info\n\n");
        sb.append("**APP_NAME**: `Gas Leak Detector`  \n");
        sb.append("**PACKAGE_NAME**: `").append(pkgName).append("`  \n");
        sb.append("**VERSION_NAME**: `").append(versionName).append("`  \n");
        sb.append("**VERSION_CODE**: `").append(versionCode).append("`  \n");
        sb.append("**TARGET_SDK**: `").append(Build.VERSION.SDK_INT).append("`  \n");
        sb.append("**IS_DEBUGGABLE_BUILD**: `false`  \n");
        sb.append("##\n\n\n");

        sb.append("## Device Info\n\n");
        sb.append("### Software\n\n");
        sb.append("**OS_VERSION**: `").append(System.getProperty("os.version", "-")).append("`  \n");
        sb.append("**SDK_INT**: `").append(Build.VERSION.SDK_INT).append("`  \n");
        sb.append("**RELEASE**: `").append(Build.VERSION.RELEASE).append("`  \n");
        sb.append("**ID**: `").append(Build.ID).append("`  \n");
        sb.append("**DISPLAY**: `").append(Build.DISPLAY).append("`  \n");
        sb.append("**INCREMENTAL**: `").append(Build.VERSION.INCREMENTAL).append("`  \n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sb.append("**SECURITY_PATCH**: `").append(Build.VERSION.SECURITY_PATCH).append("`  \n");
        }
        sb.append("**TYPE**: `").append(Build.TYPE).append("`  \n");
        sb.append("**TAGS**: `").append(Build.TAGS).append("`  \n");
        sb.append("\n### Hardware\n\n");
        sb.append("**MANUFACTURER**: `").append(Build.MANUFACTURER).append("`  \n");
        sb.append("**BRAND**: `").append(Build.BRAND).append("`  \n");
        sb.append("**MODEL**: `").append(Build.MODEL).append("`  \n");
        sb.append("**PRODUCT**: `").append(Build.PRODUCT).append("`  \n");
        sb.append("**BOARD**: `").append(Build.BOARD).append("`  \n");
        sb.append("**HARDWARE**: `").append(Build.HARDWARE).append("`  \n");
        sb.append("**DEVICE**: `").append(Build.DEVICE).append("`  \n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sb.append("**SUPPORTED_ABIS**: `")
              .append(join(", ", Build.SUPPORTED_ABIS))
              .append("`  \n");
        }
        sb.append("##\n");

        return sb.toString();
    }

    // --- File + Activity launch ---

    private static boolean isCrashFromReportActivity(Throwable throwable) {
        String reportActivityName = CrashReportActivity.class.getName();
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                // Direct frame match: crash originated inside CrashReportActivity.
                if (frame.getClassName().startsWith(reportActivityName)) {
                    return true;
                }
                // BadTokenException from ActivityThread means a window from our
                // Activity was being attached when the token was already invalid.
                // CrashReportActivity has no frames in this trace, but the symptom
                // is still a crash loop so we must not re-launch.
                if (throwable instanceof android.view.WindowManager.BadTokenException
                        && frame.getClassName().contains("ActivityThread")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void writeReportToCache(Context app, String report) throws Exception {
        // If a previous report was never read (i.e. CrashReportActivity was
        // killed before the user could copy it), keep the original file so the
        // user still sees the first crash rather than the secondary one.
        File file = new File(app.getCacheDir(), REPORT_FILE_NAME);
        if (file.exists()) file.delete();
        FileOutputStream fos = new FileOutputStream(file, false);
        fos.write(report.getBytes("UTF-8"));
        fos.close();
    }

    private static void launchReportActivity(Context app) {
        Intent intent = new Intent(app, CrashReportActivity.class);
        intent.putExtra(REPORT_FILE_KEY, new File(app.getCacheDir(), REPORT_FILE_NAME).getAbsolutePath());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        app.startActivity(intent);
    }

    // --- Helpers ---

    private static String join(String sep, String[] arr) {
        if (arr == null || arr.length == 0) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(sep);
            sb.append(arr[i]);
        }
        return sb.toString();
    }
}
