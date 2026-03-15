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
package com.gasleak.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.gasleak.R;
import com.gasleak.data.local.LocalDataStorage;
import com.gasleak.data.prefs.SharedPrefs;
import com.gasleak.util.LocaleHelper;
import com.gasleak.util.ThemeUtil;

public class SettingActivity extends AppCompatActivity {

    private Switch   notificationSwitch;
    private Switch   autoRefreshSwitch;
    private Switch   autoStreamSwitch;
    private Switch   keepAppRunningSwitch;
    private View     clearCacheButton;
    private View     resetDefaultsButton;
    private View     languageButton;
    private TextView languageValueText;

    private SharedPrefs      sharedPrefs;
    private LocalDataStorage localStorage;

    private static final String[] LANG_CODES  = {"en", "vi", "zh", "ja", "ko", "fr", "es", "de"};
    private static final String[] LANG_LABELS = {"English", "Tiếng Việt", "中文", "日本語", "한국어", "Français", "Español", "Deutsch"};

    @Override
    protected void attachBaseContext(Context base) {
        String lang = new SharedPrefs(base).getLanguage();
        super.attachBaseContext(LocaleHelper.applyLocale(base, lang));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.applyTheme(this);
        super.onCreate(savedInstanceState);
        sharedPrefs  = new SharedPrefs(this);
        localStorage = new LocalDataStorage(this);
        setContentView(R.layout.activity_setting);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        notificationSwitch   = findViewById(R.id.notificationSwitch);
        autoRefreshSwitch    = findViewById(R.id.autoRefreshSwitch);
        autoStreamSwitch     = findViewById(R.id.autoStreamSwitch);
        keepAppRunningSwitch = findViewById(R.id.keepAppRunningSwitch);
        clearCacheButton     = findViewById(R.id.clearCacheButton);
        resetDefaultsButton  = findViewById(R.id.resetDefaultsButton);
        languageButton       = findViewById(R.id.languageButton);
        languageValueText    = findViewById(R.id.languageValueText);
    }

    private void loadSettings() {
        notificationSwitch.setChecked(sharedPrefs.getNotificationsEnabled());
        autoRefreshSwitch.setChecked(sharedPrefs.getAutoRefreshEnabled());
        autoStreamSwitch.setChecked(sharedPrefs.getAutoStreamEnabled());
        keepAppRunningSwitch.setChecked(sharedPrefs.getKeepAppRunning());
        updateLanguageLabel();
    }

    private void updateLanguageLabel() {
        String current = sharedPrefs.getLanguage();
        for (int i = 0; i < LANG_CODES.length; i++) {
            if (LANG_CODES[i].equals(current)) { languageValueText.setText(LANG_LABELS[i]); return; }
        }
        languageValueText.setText(LANG_LABELS[0]);
    }

    private void setupListeners() {
        notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean isChecked) {
                sharedPrefs.setNotificationsEnabled(isChecked);
                Toast.makeText(SettingActivity.this,
                    getString(isChecked ? R.string.toast_notifications_enabled : R.string.toast_notifications_disabled),
                    Toast.LENGTH_SHORT).show();
            }
        });

        autoRefreshSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean isChecked) {
                sharedPrefs.setAutoRefreshEnabled(isChecked);
                Toast.makeText(SettingActivity.this,
                    getString(isChecked ? R.string.toast_auto_refresh_enabled : R.string.toast_auto_refresh_disabled),
                    Toast.LENGTH_SHORT).show();
            }
        });

        autoStreamSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean isChecked) {
                sharedPrefs.setAutoStreamEnabled(isChecked);
                Toast.makeText(SettingActivity.this,
                    getString(isChecked ? R.string.toast_auto_stream_enabled : R.string.toast_auto_stream_disabled),
                    Toast.LENGTH_SHORT).show();
            }
        });

        keepAppRunningSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean isChecked) {
                sharedPrefs.setKeepAppRunning(isChecked);
                Toast.makeText(SettingActivity.this,
                    getString(isChecked ? R.string.toast_keep_running_enabled : R.string.toast_keep_running_disabled),
                    Toast.LENGTH_SHORT).show();
            }
        });

        clearCacheButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showClearCacheDialog(); }
        });

        resetDefaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showResetDefaultsDialog(); }
        });

        languageButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showLanguageDialog(); }
        });
    }

    private void showLanguageDialog() {
        String current = sharedPrefs.getLanguage();
        final int[] selected = {0};
        for (int i = 0; i < LANG_CODES.length; i++) {
            if (LANG_CODES[i].equals(current)) { selected[0] = i; break; }
        }
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.label_language))
            .setSingleChoiceItems(LANG_LABELS, selected[0], new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { selected[0] = which; }
            })
            .setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newLang = LANG_CODES[selected[0]];
                    if (newLang.equals(sharedPrefs.getLanguage())) return;
                    sharedPrefs.setLanguage(newLang);
                    Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    private void showClearCacheDialog() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_cache_title))
            .setMessage(getString(R.string.clear_cache_message, localStorage.getCacheInfo()))
            .setPositiveButton(getString(R.string.btn_clear), new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { localStorage.clearCache(); }
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    private void showResetDefaultsDialog() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.reset_defaults_title))
            .setMessage(getString(R.string.reset_defaults_message))
            .setPositiveButton(getString(R.string.btn_reset), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sharedPrefs.resetToDefaults();
                    loadSettings();
                    Toast.makeText(SettingActivity.this, getString(R.string.toast_settings_reset), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_down_out);
    }
}
