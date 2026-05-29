/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Modified: 2026-05-29
 */
package com.gasleakdetector.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;

/**
 * Displays the crash report written by {@link CrashHandler} and lets the
 * user copy it or open a GitHub issue.
 *
 * The report file is read from getCacheDir() using the path passed in the
 * launching intent. The file is deleted once the Activity is shown so it
 * does not persist across sessions.
 *
 * This Activity must be declared in AndroidManifest.xml:
 *
 *   <activity
 *       android:name=".util.CrashReportActivity"
 *       android:exported="false"
 *       android:theme="@android:style/Theme.Material.Light.Dialog.Alert"
 *       android:documentLaunchMode="intoExisting" />
 */
public class CrashReportActivity extends Activity {

    private static final String TAG           = "CrashReportActivity";
    private static final String GITHUB_ISSUES =
        "https://github.com/gasleakdetector/gasleakdetector/issues/new";

    private String mReportText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReportText = readAndDeleteReportFile();

        buildUi();
    }

    // --- UI ---

    private void buildUi() {
        // Root layout
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 0);

        // Title bar
        TextView title = new TextView(this);
        title.setText("App Crash Report");
        title.setTextSize(18f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(32, 32, 32, 16);

        // Report text inside a ScrollView
        TextView tv = new TextView(this);
        tv.setText(mReportText.isEmpty() ? "(no report found)" : mReportText);
        tv.setTextSize(11f);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setPadding(32, 16, 32, 16);
        tv.setTextIsSelectable(true);

        ScrollView scroll = new ScrollView(this);
        android.widget.LinearLayout.LayoutParams scrollParams =
            new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(scrollParams);
        scroll.addView(tv);

        // Button row
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setPadding(16, 8, 16, 16);

        Button btnCopyIssue = new Button(this);
        btnCopyIssue.setText("Copy & Open Issue");
        android.widget.LinearLayout.LayoutParams btnParams =
            new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnParams.setMargins(4, 0, 4, 0);
        btnCopyIssue.setLayoutParams(btnParams);

        Button btnCopy = new Button(this);
        btnCopy.setText("Copy");
        android.widget.LinearLayout.LayoutParams btnParams2 =
            new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnParams2.setMargins(4, 0, 4, 0);
        btnCopy.setLayoutParams(btnParams2);

        Button btnClose = new Button(this);
        btnClose.setText("Close");
        android.widget.LinearLayout.LayoutParams btnParams3 =
            new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnParams3.setMargins(4, 0, 4, 0);
        btnClose.setLayoutParams(btnParams3);

        btnRow.addView(btnCopyIssue);
        btnRow.addView(btnCopy);
        btnRow.addView(btnClose);

        root.addView(title);
        root.addView(scroll);
        root.addView(btnRow);

        setContentView(root);

        // Listeners
        final String reportSnapshot = mReportText;

        btnCopyIssue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(reportSnapshot);
                openGithubIssue();
                finish();
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(reportSnapshot);
                Toast.makeText(CrashReportActivity.this,
                    "Copied to clipboard", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // --- Helpers ---

    private String readAndDeleteReportFile() {
        String path = null;
        Intent intent = getIntent();
        if (intent != null) {
            path = intent.getStringExtra(CrashHandler.REPORT_FILE_KEY);
        }

        if (path == null) return "";

        File file = new File(path);
        if (!file.exists()) return "";

        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String content = new String(data, "UTF-8");
            file.delete();
            return content;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read crash report file", e);
            file.delete();
            return "";
        }
    }

    private void copyToClipboard(String text) {
        try {
            ClipboardManager cm = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Crash Report", text));
            }
        } catch (Throwable ignored) {}
    }

    private void openGithubIssue() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_ISSUES))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) {}
    }
}
