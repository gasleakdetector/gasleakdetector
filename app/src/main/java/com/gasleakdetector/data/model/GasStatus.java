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

public class GasStatus {

    public static final int LEVEL_NORMAL  = 0;
    public static final int LEVEL_WARNING = 1;
    public static final int LEVEL_DANGER  = 2;

    /* Concentration thresholds — single source of truth;
     * referenced by gauge, chart, and notifications. */
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
     * Derives a level constant from a raw ppm value.
     * All code should call this instead of duplicating the threshold comparison.
     */
    public static int calculateLevel(int ppm) {
        if (ppm >= DANGER_THRESHOLD)  return LEVEL_DANGER;
        if (ppm >= WARNING_THRESHOLD) return LEVEL_WARNING;
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
