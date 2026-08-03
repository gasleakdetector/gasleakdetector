/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-01-08
 */
package com.gasleakdetector.util;

/**
 * Shared ISO 8601 date/time normalization utilities.
 *
 * SimpleDateFormat cannot parse:
 * - Colon in the timezone offset (+07:00 → +0700)
 * - Trailing Z suffix (Z → +0000)
 * - Microsecond fractional seconds (truncated to 3 digits)
 */
public class DateUtils {

    /**
     * Normalizes an ISO 8601 string so it can be parsed by SimpleDateFormat.
     * Strips the colon from the offset, converts Z to +0000, and truncates
     * microsecond fractional seconds to milliseconds.
     */
    public static String normalizeIso8601(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        String normalized = raw;
        try {
            if (normalized.length() > 6) {
                String tail = normalized.substring(normalized.length() - 6);
                if (tail.matches("[+-]\\d{2}:\\d{2}")) {
                    normalized = normalized.substring(0, normalized.length() - 6)
                            + tail.replace(":", "");
                }
            }
            if (normalized.endsWith("Z")) {
                normalized = normalized.substring(0, normalized.length() - 1) + "+0000";
            }
            // Supabase returns microseconds (6 digits after dot). SimpleDateFormat
            // only handles milliseconds (3 digits). Truncate to 3 digits.
            normalized = normalized.replaceAll("(\\.\\d{3})\\d+", "$1");
        } catch (Exception ignored) {}
        return normalized;
    }

    private DateUtils() {}
}