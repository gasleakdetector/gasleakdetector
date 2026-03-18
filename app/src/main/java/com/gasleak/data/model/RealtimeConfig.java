/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-03-17
 */
package com.gasleak.data.model;

import java.util.Objects;

public class RealtimeConfig {

    private String apiUrl;
    private String apiKey;
    private String deviceId;

    public RealtimeConfig(String apiUrl, String apiKey, String deviceId) {
        this.apiUrl   = apiUrl;
        this.apiKey   = apiKey;
        this.deviceId = deviceId;
    }

    public String getApiUrl()              { return apiUrl; }
    public void   setApiUrl(String v)      { apiUrl = v; }

    public String getApiKey()              { return apiKey; }
    public void   setApiKey(String v)      { apiKey = v; }

    public String getDeviceId()            { return deviceId; }
    public void   setDeviceId(String v)    { deviceId = v; }

    public boolean isValid() {
        return apiUrl != null && !apiUrl.isEmpty()
            && apiKey != null && !apiKey.isEmpty();
    }

    public boolean hasSameParams(RealtimeConfig other) {
        if (other == null) return false;
        return Objects.equals(apiUrl,   other.apiUrl)
            && Objects.equals(apiKey,   other.apiKey)
            && Objects.equals(deviceId, other.deviceId);
    }
}
