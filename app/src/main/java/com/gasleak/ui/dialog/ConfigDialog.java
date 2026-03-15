/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-03-15
 */
package com.gasleak.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.gasleak.R;
import com.gasleak.data.model.RealtimeConfig;

/** Dialog for entering/editing the API URL, API key, and device ID. */
public class ConfigDialog extends Dialog {

    public interface OnConfigSavedListener {
        void onConfigSaved(RealtimeConfig config);
    }

    private EditText              etApiUrl;
    private EditText              etApiKey;
    private EditText              etDeviceId;
    private final OnConfigSavedListener listener;
    private final RealtimeConfig        currentConfig;

    public ConfigDialog(Context context, RealtimeConfig currentConfig, OnConfigSavedListener listener) {
        super(context);
        this.currentConfig = currentConfig;
        this.listener      = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_config);

        etApiUrl   = findViewById(R.id.et_api_url);
        etApiKey   = findViewById(R.id.et_api_key);
        etDeviceId = findViewById(R.id.et_device_id);
        Button btnSave = findViewById(R.id.btn_save);

        if (currentConfig != null) {
            etApiUrl.setText(currentConfig.getApiUrl());
            etApiKey.setText(currentConfig.getApiKey());
            etDeviceId.setText(currentConfig.getDeviceId());
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { saveConfig(); }
        });
    }

    private void saveConfig() {
        String apiUrl   = etApiUrl.getText().toString().trim();
        String apiKey   = etApiKey.getText().toString().trim();
        String deviceId = etDeviceId.getText().toString().trim();

        if (TextUtils.isEmpty(apiUrl)) {
            Toast.makeText(getContext(), getContext().getString(R.string.error_api_url_required), Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(apiKey)) {
            Toast.makeText(getContext(), getContext().getString(R.string.error_api_key_required), Toast.LENGTH_SHORT).show();
            return;
        }

        if (listener != null) listener.onConfigSaved(new RealtimeConfig(apiUrl, apiKey, deviceId));
        dismiss();
    }
}
