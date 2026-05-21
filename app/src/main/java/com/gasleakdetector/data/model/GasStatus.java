/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-05-20
 */
package com.gasleakdetector.data.model;

public class GasStatus {

    public static final int LEVEL_NORMAL  = 0;
    public static final int LEVEL_WARNING = 1;
    public static final int LEVEL_DANGER  = 2;

    /* Default concentration thresholds — single source of truth.
     * Override via SharedPrefs.getWarningThreshold() / getDangerThreshold(). */
    public static final int WARNING_THRESHOLD = 300;
    public static final int DANGER_THRESHOLD  = 800;

    private final int    level;
    private final int    concentration;
    private final long   timestamp;
    private final String message;

    public GasStatus(int level, int concentration, long timestamp, String message) {
        this.level         = level;
        this.concentration = concentration;
        this.timestamp     = timestamp;
        this.message       = message;
    }

    /**
     * Derives a level constant using default thresholds.
     * Prefer {@link #calculateLevel(int, int, int)} when user-configured thresholds are available.
     */
    public static int calculateLevel(int ppm) {
        return calculateLevel(ppm, WARNING_THRESHOLD, DANGER_THRESHOLD);
    }

    /**
     * Derives a level constant using caller-supplied thresholds.
     * Pass values from SharedPrefs.getWarningThreshold() / getDangerThreshold().
     */
    public static int calculateLevel(int ppm, int warningThreshold, int dangerThreshold) {
        if (ppm >= dangerThreshold)  return LEVEL_DANGER;
        if (ppm >= warningThreshold) return LEVEL_WARNING;
        return LEVEL_NORMAL;
    }

    public int    getLevel()         { return level; }
    public int    getConcentration() { return concentration; }
    public long   getTimestamp()     { return timestamp; }
    public String getMessage()       { return message; }

    public boolean isNormal()  { return level == LEVEL_NORMAL; }
    public boolean isWarning() { return level == LEVEL_WARNING; }
    public boolean isDanger()  { return level == LEVEL_DANGER; }
}
