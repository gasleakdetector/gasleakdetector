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

public class HistoricalDataPointTest {

    @Test
    public void constructor_withPpmAndDate_setsAllFields() {
        HistoricalDataPoint p = new HistoricalDataPoint(450, "2026-03-15T10:00:00+0000");
        assertEquals(450, p.getGasPpm());
        assertEquals(450f, p.getAvgGas(), 0.001f);
        assertEquals(450f, p.getMinGas(), 0.001f);
        assertEquals(450f, p.getMaxGas(), 0.001f);
        assertEquals("2026-03-15T10:00:00+0000", p.getCreatedAt());
    }

    @Test
    public void getGasPpm_whenGasPpmZero_fallsBackToAvgGas() {
        HistoricalDataPoint p = new HistoricalDataPoint();
        p.setGasPpm(0);
        p.setAvgGas(320f);
        assertEquals(320, p.getGasPpm());
    }

    @Test
    public void getGasPpm_whenGasPpmNonZero_returnsGasPpm() {
        HistoricalDataPoint p = new HistoricalDataPoint();
        p.setGasPpm(500);
        p.setAvgGas(999f);
        assertEquals(500, p.getGasPpm());
    }

    @Test
    public void getCreatedAt_whenBucketSet_andCreatedAtNull_returnsBucket() {
        HistoricalDataPoint p = new HistoricalDataPoint();
        p.setBucket("2026-03-15T00:00:00+0000");
        assertEquals("2026-03-15T00:00:00+0000", p.getCreatedAt());
    }

    @Test
    public void getCreatedAt_prefersCreatedAtOverBucket() {
        HistoricalDataPoint p = new HistoricalDataPoint();
        p.setCreatedAt("2026-03-10T00:00:00+0000");
        p.setBucket("2026-03-15T00:00:00+0000");
        // createdAt is already set so it should be preferred
        assertEquals("2026-03-10T00:00:00+0000", p.getCreatedAt());
    }

    @Test
    public void setters_andGetters_roundTrip() {
        HistoricalDataPoint p = new HistoricalDataPoint();
        p.setId(42L);
        p.setDeviceId("esp32-001");
        p.setGasPpm(700);
        p.setStatus("WARNING");
        p.setIpAddress("192.168.1.10");
        p.setSampleCount(10);
        p.setMinGas(600f);
        p.setMaxGas(750f);
        p.setAvgGas(680f);

        assertEquals(42L, p.getId());
        assertEquals("esp32-001", p.getDeviceId());
        assertEquals(700, p.getGasPpm());
        assertEquals("WARNING", p.getStatus());
        assertEquals("192.168.1.10", p.getIpAddress());
        assertEquals(10, p.getSampleCount());
        assertEquals(600f, p.getMinGas(), 0.001f);
        assertEquals(750f, p.getMaxGas(), 0.001f);
        assertEquals(680f, p.getAvgGas(), 0.001f);
    }

    @Test
    public void getTimestamp_withNullDate_returnsCurrentTime() {
        HistoricalDataPoint p = new HistoricalDataPoint();
        long before = System.currentTimeMillis();
        long ts = p.getTimestamp();
        long after = System.currentTimeMillis();
        assertTrue(ts >= before && ts <= after);
    }

    @Test
    public void getTimestamp_withIsoDateAndZOffset_parsesCorrectly() {
        HistoricalDataPoint p = new HistoricalDataPoint();
        // 2026-01-01T00:00:00Z = 1767225600000 ms
        p.setCreatedAt("2026-01-01T00:00:00Z");
        assertEquals(1767225600000L, p.getTimestamp());
    }

    @Test
    public void getTimestamp_withColonOffset_parsesCorrectly() {
        HistoricalDataPoint p = new HistoricalDataPoint();
        // 2026-01-01T07:00:00+07:00 → UTC midnight = 1767225600000 ms
        p.setCreatedAt("2026-01-01T07:00:00+07:00");
        assertEquals(1767225600000L, p.getTimestamp());
    }

    @Test
    public void getTimestamp_withDateOnly_parsesCorrectly() {
        HistoricalDataPoint p = new HistoricalDataPoint();
        // 2026-01-01 (date-only parsed as UTC midnight)
        p.setCreatedAt("2026-01-01");
        assertEquals(1767225600000L, p.getTimestamp());
    }

    @Test
    public void getTimestamp_bucketTakesPriorityOverCreatedAt() {
        HistoricalDataPoint p = new HistoricalDataPoint();
        p.setCreatedAt("2026-01-01T00:00:00Z");
        p.setBucket("2026-02-01T00:00:00Z");
        // bucket is set → getTimestamp() uses bucket = 1769904000000 ms
        assertEquals(1769904000000L, p.getTimestamp());
    }
}
