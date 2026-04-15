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
package com.gasleakdetector.data.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class RealtimeConfigTest {

    @Test
    public void isValid_withAllFields_returnsTrue() {
        RealtimeConfig config = new RealtimeConfig("https://api.example.com", "key123", "device-01");
        assertTrue(config.isValid());
    }

    @Test
    public void isValid_withNullUrl_returnsFalse() {
        RealtimeConfig config = new RealtimeConfig(null, "key123", "device-01");
        assertFalse(config.isValid());
    }

    @Test
    public void isValid_withEmptyUrl_returnsFalse() {
        RealtimeConfig config = new RealtimeConfig("", "key123", "device-01");
        assertFalse(config.isValid());
    }

    @Test
    public void isValid_withNullApiKey_returnsFalse() {
        RealtimeConfig config = new RealtimeConfig("https://api.example.com", null, "device-01");
        assertFalse(config.isValid());
    }

    @Test
    public void isValid_withEmptyApiKey_returnsFalse() {
        RealtimeConfig config = new RealtimeConfig("https://api.example.com", "", "device-01");
        assertFalse(config.isValid());
    }

    @Test
    public void isValid_withNullDeviceId_returnsTrue() {
        // deviceId is optional; validity depends only on url + apiKey
        RealtimeConfig config = new RealtimeConfig("https://api.example.com", "key123", null);
        assertTrue(config.isValid());
    }

    @Test
    public void setters_updateValues() {
        RealtimeConfig config = new RealtimeConfig("old-url", "old-key", "old-device");
        config.setApiUrl("new-url");
        config.setApiKey("new-key");
        config.setDeviceId("new-device");

        assertEquals("new-url", config.getApiUrl());
        assertEquals("new-key", config.getApiKey());
        assertEquals("new-device", config.getDeviceId());
    }

    @Test
    public void getters_returnConstructorValues() {
        RealtimeConfig config = new RealtimeConfig("https://api.example.com", "key123", "device-01");
        assertEquals("https://api.example.com", config.getApiUrl());
        assertEquals("key123", config.getApiKey());
        assertEquals("device-01", config.getDeviceId());
    }
}
