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
package com.gasleakdetector.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.gasleakdetector.R;
import com.gasleakdetector.data.prefs.SharedPrefs;

/**
 * Full-screen welcome/onboarding screen shown only on the very first launch.
 *
 * Flow:
 *   First launch  → WelcomeActivity (launcher) → user taps Continue
 *                 → sets intro_shown = true → starts MainActivity → finish()
 *   Later launches → MainActivity directly (WelcomeActivity is never started)
 *
 * The "already seen" check lives in MainActivity.onCreate() so that the
 * Manifest launcher activity never needs to change.
 */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the user has already seen the intro, skip straight to the main screen.
        SharedPrefs prefs = new SharedPrefs(this);
        if (prefs.isIntroShown()) {
            launchMain();
            return;
        }

        setContentView(R.layout.activity_welcome);

        Button btnContinue = findViewById(R.id.btn_continue);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.setIntroShown(true);
                launchMain();
            }
        });
    }

    private void launchMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
