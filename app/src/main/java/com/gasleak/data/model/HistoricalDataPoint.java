/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/traitimtrongvag/gasleakdetector-app
 * Modified: 2026-03-15
 */
package com.gasleak.data.model;

import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Represents a single gas measurement from the API.
 *
 * The API returns two shapes:
 *   - Raw rows: gas_ppm + created_at
 *   - Summary buckets: avg/min/max + bucket timestamp
 * Both shapes are mapped into this class for uniform handling downstream.
 */
public class HistoricalDataPoint {

    private static final String TAG = "HistoricalDataPoint";

    // ── Raw row fields ────────────────────────────────────────────────────────
    private long   id;
    private String deviceId;
    private int    gasPpm;
    private String status;
    private String ipAddress;
    private String createdAt;

    // ── Summary/aggregate fields ──────────────────────────────────────────────
    private String bucket;
    private float  avgGas;
    private float  minGas;
    private float  maxGas;
    private int    sampleCount;

    public HistoricalDataPoint() {}

    public HistoricalDataPoint(int gasPpm, String createdAt) {
        this.gasPpm    = gasPpm;
        this.createdAt = createdAt;
        this.avgGas    = gasPpm;
        this.minGas    = gasPpm;
        this.maxGas    = gasPpm;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public long   getId()              { return id; }
    public void   setId(long v)        { id = v; }

    public String getDeviceId()        { return deviceId; }
    public void   setDeviceId(String v){ deviceId = v; }

    public int    getGasPpm()          { return gasPpm != 0 ? gasPpm : (int) avgGas; }
    public void   setGasPpm(int v)     { gasPpm = v; }

    public String getStatus()          { return status; }
    public void   setStatus(String v)  { status = v; }

    public String getIpAddress()       { return ipAddress; }
    public void   setIpAddress(String v){ ipAddress = v; }

    public String getCreatedAt()       { return createdAt != null ? createdAt : bucket; }
    public void   setCreatedAt(String v){ createdAt = v; }

    public String getBucket()          { return bucket; }
    public void   setBucket(String v)  { bucket = v; if (createdAt == null) createdAt = v; }

    public float  getAvgGas()          { return avgGas; }
    public void   setAvgGas(float v)   { avgGas = v; }

    public float  getMinGas()          { return minGas; }
    public void   setMinGas(float v)   { minGas = v; }

    public float  getMaxGas()          { return maxGas; }
    public void   setMaxGas(float v)   { maxGas = v; }

    public int    getSampleCount()     { return sampleCount; }
    public void   setSampleCount(int v){ sampleCount = v; }

    // ── Timestamp parsing ─────────────────────────────────────────────────────

    /**
     * Returns a Unix timestamp in milliseconds from the ISO 8601 date string.
     * Falls back to the current time if parsing fails.
     */
    public long getTimestamp() {
        String raw = bucket != null ? bucket : createdAt;
        if (raw == null || raw.isEmpty()) return System.currentTimeMillis();

        // ISO 8601 with a colon in the offset (e.g. +07:00) isn't supported by SimpleDateFormat;
        // strip the colon to get +0700.
        String normalized = raw;
        try {
            if (normalized.length() > 6) {
                String tail = normalized.substring(normalized.length() - 6);
                if (tail.matches("[+-]\\d{2}:\\d{2}")) {
                    normalized = normalized.substring(0, normalized.length() - 6) + tail.replace(":", "");
                }
            }
            if (normalized.endsWith("Z")) {
                normalized = normalized.substring(0, normalized.length() - 1) + "+0000";
            }
        } catch (Exception ignored) {}

        Date d;
        d = tryParse(normalized, "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"); if (d != null) return d.getTime();
        d = tryParse(normalized, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");    if (d != null) return d.getTime();
        d = tryParse(normalized, "yyyy-MM-dd'T'HH:mm:ssZ");         if (d != null) return d.getTime();
        d = tryParse(raw,        "yyyy-MM-dd");                      if (d != null) return d.getTime();

        Log.w(TAG, "Cannot parse timestamp: " + raw);
        return System.currentTimeMillis();
    }

    private static Date tryParse(String input, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(input);
        } catch (ParseException | IllegalArgumentException e) {
            return null;
        }
    }
}
