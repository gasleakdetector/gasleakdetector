/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-05-28
 */
package com.gasleakdetector.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Catches uncaught exceptions and shows a copyable crash report dialog
 * instead of writing to disk.
 *
 * The report format mirrors the Termux issue report style so it can be
 * pasted directly into a GitHub issue without reformatting.
 *
 * Why no file write: internal storage writes can silently fail on
 * low-storage devices and the file is inaccessible without root or
 * developer options. A dialog lets the user copy and share immediately.
 *
 * Why delegate to DEFAULT_HANDLER after showing the dialog: skipping it
 * leaves the main thread stuck, suppresses Android's own crash signal,
 * and prevents the system from cleaning up the process correctly.
 */
public final class CrashHandler {

    private static final String TAG            = "CrashHandler";
    private static final String GITHUB_ISSUES  = "https://github.com/gasleakdetector/gasleakdetector/issues/new";

    public static void init(final Context app) {
        final Thread.UncaughtExceptionHandler defaultHandler =
            Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread thread, final Throwable throwable) {
                try {
                    final String report = buildReport(app, throwable);
                    Log.e(TAG, report);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            showCrashDialog(app, report, thread, throwable, defaultHandler);
                        }
                    });

                } catch (Throwable secondary) {
                    Log.e(TAG, "CrashHandler failed to build report", secondary);
                    delegateToDefault(defaultHandler, thread, throwable);
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

        String appName     = "Gas Leak Detector";
        String pkgName     = app.getPackageName();
        String versionName = "unknown";
        long   versionCode = 0;
        String targetSdk   = String.valueOf(Build.VERSION.SDK_INT);
        String isDebug     = "false";

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
        sb.append("**APP_NAME**: `").append(appName).append("`  \n");
        sb.append("**PACKAGE_NAME**: `").append(pkgName).append("`  \n");
        sb.append("**VERSION_NAME**: `").append(versionName).append("`  \n");
        sb.append("**VERSION_CODE**: `").append(versionCode).append("`  \n");
        sb.append("**TARGET_SDK**: `").append(targetSdk).append("`  \n");
        sb.append("**IS_DEBUGGABLE_BUILD**: `").append(isDebug).append("`  \n");
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

    // --- Dialog ---

    private static void showCrashDialog(
            final Context app,
            final String report,
            final Thread thread,
            final Throwable throwable,
            final Thread.UncaughtExceptionHandler defaultHandler) {

        // Find a live Activity context for the dialog; fall back to app context.
        Context dialogCtx = app;
        if (app instanceof Activity && !((Activity) app).isFinishing()) {
            dialogCtx = app;
        }

        TextView tv = new TextView(dialogCtx);
        tv.setText(report);
        tv.setTextSize(11f);
        tv.setPadding(32, 24, 32, 24);
        tv.setTextIsSelectable(true);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);

        ScrollView scroll = new ScrollView(dialogCtx);
        scroll.addView(tv);

        final Context ctx = dialogCtx;
        new AlertDialog.Builder(ctx)
            .setTitle("App Crash Report")
            .setView(scroll)
            .setCancelable(false)
            .setPositiveButton("Copy & Open Issue", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    copyToClipboard(ctx, report);
                    openGithubIssue(ctx);
                    delegateToDefault(defaultHandler, thread, throwable);
                }
            })
            .setNegativeButton("Copy", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    copyToClipboard(ctx, report);
                    Toast.makeText(ctx, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    delegateToDefault(defaultHandler, thread, throwable);
                }
            })
            .setNeutralButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    delegateToDefault(defaultHandler, thread, throwable);
                }
            })
            .show();
    }

    // --- Helpers ---

    private static void copyToClipboard(Context ctx, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Crash Report", text));
        } catch (Throwable ignored) {}
    }

    private static void openGithubIssue(Context ctx) {
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_ISSUES))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) {}
    }

    private static void delegateToDefault(
            Thread.UncaughtExceptionHandler handler,
            Thread thread,
            Throwable throwable) {
        if (handler != null) {
            handler.uncaughtException(thread, throwable);
        } else {
            System.exit(1);
        }
    }

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
