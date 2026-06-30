/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Modified: 2026-06-27
 */
package com.gasleakdetector.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gasleakdetector.R;

import java.io.File;
import java.io.FileInputStream;

/**
 * Displays the crash report written by {@link CrashHandler} and lets the
 * user copy it or open a GitHub issue.
 *
 * The report file is read from getCacheDir() using the path passed in the
 * launching intent. The file is deleted once the Activity is shown so it
 * does not persist across sessions.
 */
public class CrashReportActivity extends AppCompatActivity {

    private static final String TAG           = "CrashReportActivity";
    private static final String GITHUB_ISSUES =
        "https://github.com/gasleakdetector/gasleakdetector/issues/new";

    private String mReportText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_report);

        mReportText = readAndDeleteReportFile();

        buildUi();
    }

    // --- UI ---

    private void buildUi() {
        TextView reportView = findViewById(R.id.report_text);
        reportView.setText(mReportText.isEmpty()
            ? getString(R.string.no_report_found) : mReportText);

        final String reportSnapshot = mReportText;

        Button btnCopyIssue = findViewById(R.id.btn_copy_issue);
        btnCopyIssue.setOnClickListener(v -> {
            copyToClipboard(reportSnapshot);
            openGithubIssue();
            finish();
        });

        Button btnCopy = findViewById(R.id.btn_copy);
        btnCopy.setOnClickListener(v -> {
            copyToClipboard(reportSnapshot);
            Toast.makeText(CrashReportActivity.this,
                R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            finish();
        });

        Button btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> finish());
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
