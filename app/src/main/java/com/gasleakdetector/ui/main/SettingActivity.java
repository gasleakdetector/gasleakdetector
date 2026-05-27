/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-05-26
 */
package com.gasleakdetector.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.gasleakdetector.R;
import com.gasleakdetector.data.local.LocalDataStorage;
import com.gasleakdetector.data.model.GasStatus;
import com.gasleakdetector.data.prefs.SharedPrefs;
import com.gasleakdetector.util.LocaleHelper;
import com.gasleakdetector.util.ThemeUtil;

public class SettingActivity extends AppCompatActivity {

    /* ------------------------------------------------------------------ */
    /*  Views                                                               */
    /* ------------------------------------------------------------------ */

    private Switch   notificationSwitch;
    private Switch   autoRefreshSwitch;
    private Switch   autoStreamSwitch;
    private Switch   keepAppRunningSwitch;
    private View     clearCacheButton;
    private View     resetDefaultsButton;
    private View     languageButton;
    private TextView languageValueText;
    private View     warningThresholdButton;
    private View     dangerThresholdButton;
    private TextView warningThresholdValue;
    private TextView dangerThresholdValue;
    private View     alertMinLevelButton;
    private View     alertDelayButton;
    private TextView alertMinLevelValue;
    private TextView alertDelayValue;

    /* ------------------------------------------------------------------ */
    /*  Dependencies                                                        */
    /* ------------------------------------------------------------------ */

    private SharedPrefs      sharedPrefs;
    private LocalDataStorage localStorage;

    private static final String[] LANG_CODES  = {"en", "vi", "zh", "ja", "ko", "fr", "es", "de"};
    private static final String[] LANG_LABELS = {"English", "Tieng Viet", "Zhongwen", "Nihongo", "Hangugeo", "Francais", "Espanol", "Deutsch"};

    /* ------------------------------------------------------------------ */
    /*  Lifecycle                                                           */
    /* ------------------------------------------------------------------ */

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

    /* ------------------------------------------------------------------ */
    /*  Private - init                                                      */
    /* ------------------------------------------------------------------ */

    private void initViews() {
        notificationSwitch   = findViewById(R.id.notificationSwitch);
        autoRefreshSwitch    = findViewById(R.id.autoRefreshSwitch);
        autoStreamSwitch     = findViewById(R.id.autoStreamSwitch);
        keepAppRunningSwitch = findViewById(R.id.keepAppRunningSwitch);
        clearCacheButton     = findViewById(R.id.clearCacheButton);
        resetDefaultsButton  = findViewById(R.id.resetDefaultsButton);
        languageButton       = findViewById(R.id.languageButton);
        languageValueText    = findViewById(R.id.languageValueText);
        warningThresholdButton = findViewById(R.id.warningThresholdButton);
        dangerThresholdButton  = findViewById(R.id.dangerThresholdButton);
        warningThresholdValue  = findViewById(R.id.warningThresholdValue);
        dangerThresholdValue   = findViewById(R.id.dangerThresholdValue);
        alertMinLevelButton  = findViewById(R.id.alertMinLevelButton);
        alertDelayButton     = findViewById(R.id.alertDelayButton);
        alertMinLevelValue   = findViewById(R.id.alertMinLevelValue);
        alertDelayValue      = findViewById(R.id.alertDelayValue);
    }

    private void loadSettings() {
        notificationSwitch.setChecked(sharedPrefs.getNotificationsEnabled());
        autoRefreshSwitch.setChecked(sharedPrefs.getAutoRefreshEnabled());
        autoStreamSwitch.setChecked(sharedPrefs.getAutoStreamEnabled());
        keepAppRunningSwitch.setChecked(sharedPrefs.getKeepAppRunning());
        updateLanguageLabel();
        updateThresholdLabels();
        updateAlertMinLevelLabel();
        updateAlertDelayLabel();
    }

    /* ------------------------------------------------------------------ */
    /*  Private - label updaters                                            */
    /* ------------------------------------------------------------------ */

    private void updateLanguageLabel() {
        String current = sharedPrefs.getLanguage();
        for (int i = 0; i < LANG_CODES.length; i++) {
            if (LANG_CODES[i].equals(current)) { languageValueText.setText(LANG_LABELS[i]); return; }
        }
        languageValueText.setText(LANG_LABELS[0]);
    }

    private void updateThresholdLabels() {
        warningThresholdValue.setText(getString(R.string.threshold_value_fmt, sharedPrefs.getWarningThreshold()));
        dangerThresholdValue.setText(getString(R.string.threshold_value_fmt, sharedPrefs.getDangerThreshold()));
    }

    private void updateAlertMinLevelLabel() {
        int level = sharedPrefs.getAlertMinLevel();
        if (level == SharedPrefs.ALERT_LEVEL_DANGER) {
            alertMinLevelValue.setText(getString(R.string.alert_level_danger));
        } else {
            alertMinLevelValue.setText(getString(R.string.alert_level_warning));
        }
    }

    private void updateAlertDelayLabel() {
        alertDelayValue.setText(getString(R.string.alert_delay_fmt, sharedPrefs.getAlertDelayMinutes()));
    }

    /* ------------------------------------------------------------------ */
    /*  Private - listeners                                                 */
    /* ------------------------------------------------------------------ */

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

        warningThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showThresholdDialog(false); }
        });

        dangerThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showThresholdDialog(true); }
        });

        alertMinLevelButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showAlertMinLevelDialog(); }
        });

        alertDelayButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showAlertDelayDialog(); }
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Private - dialogs                                                   */
    /* ------------------------------------------------------------------ */

    private void showAlertMinLevelDialog() {
        final String[] labels = {
            getString(R.string.alert_level_warning),
            getString(R.string.alert_level_danger)
        };
        final int[] levels = {
            SharedPrefs.ALERT_LEVEL_WARNING,
            SharedPrefs.ALERT_LEVEL_DANGER
        };

        int current = sharedPrefs.getAlertMinLevel();
        final int[] selected = {0};
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] == current) { selected[0] = i; break; }
        }

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.label_alert_min_level))
            .setSingleChoiceItems(labels, selected[0], new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) { selected[0] = which; }
            })
            .setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    sharedPrefs.setAlertMinLevel(levels[selected[0]]);
                    updateAlertMinLevelLabel();
                    Toast.makeText(SettingActivity.this,
                        getString(R.string.toast_alert_min_level_saved), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    private void showAlertDelayDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(sharedPrefs.getAlertDelayMinutes()));
        input.selectAll();
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_alert_delay_title))
            .setMessage(getString(R.string.dialog_alert_delay_message))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) return;
                    int value;
                    try { value = Integer.parseInt(raw); } catch (NumberFormatException e) { value = -1; }
                    if (value < 1 || value > 60) {
                        Toast.makeText(SettingActivity.this,
                            getString(R.string.toast_alert_delay_invalid), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sharedPrefs.setAlertDelayMinutes(value);
                    updateAlertDelayLabel();
                    Toast.makeText(SettingActivity.this,
                        getString(R.string.toast_alert_delay_saved), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    private void showThresholdDialog(final boolean isDanger) {
        final int current = isDanger
            ? sharedPrefs.getDangerThreshold()
            : sharedPrefs.getWarningThreshold();

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.selectAll();
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
            .setTitle(getString(isDanger ? R.string.label_danger_threshold : R.string.label_warning_threshold))
            .setMessage(getString(isDanger ? R.string.desc_danger_threshold : R.string.desc_warning_threshold))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) return;
                    int value = Integer.parseInt(raw);

                    if (!isDanger && value >= sharedPrefs.getDangerThreshold()) {
                        Toast.makeText(SettingActivity.this,
                            getString(R.string.toast_warning_must_be_less_than_danger),
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (isDanger && value <= sharedPrefs.getWarningThreshold()) {
                        Toast.makeText(SettingActivity.this,
                            getString(R.string.toast_danger_must_be_greater_than_warning),
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isDanger) sharedPrefs.setDangerThreshold(value);
                    else          sharedPrefs.setWarningThreshold(value);
                    updateThresholdLabels();
                    Toast.makeText(SettingActivity.this,
                        getString(R.string.toast_threshold_saved),
                        Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
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
