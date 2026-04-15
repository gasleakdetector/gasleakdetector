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

public class GasStatusTest {

    @Test
    public void calculateLevel_belowWarning_returnsNormal() {
        assertEquals(GasStatus.LEVEL_NORMAL, GasStatus.calculateLevel(0));
        assertEquals(GasStatus.LEVEL_NORMAL, GasStatus.calculateLevel(299));
    }

    @Test
    public void calculateLevel_atWarningThreshold_returnsWarning() {
        assertEquals(GasStatus.LEVEL_WARNING, GasStatus.calculateLevel(300));
        assertEquals(GasStatus.LEVEL_WARNING, GasStatus.calculateLevel(799));
    }

    @Test
    public void calculateLevel_atDangerThreshold_returnsDanger() {
        assertEquals(GasStatus.LEVEL_DANGER, GasStatus.calculateLevel(800));
        assertEquals(GasStatus.LEVEL_DANGER, GasStatus.calculateLevel(9999));
    }

    @Test
    public void isNormal_onNormalLevel_returnsTrue() {
        GasStatus status = new GasStatus(GasStatus.LEVEL_NORMAL, 100, 0L, "");
        assertTrue(status.isNormal());
        assertFalse(status.isWarning());
        assertFalse(status.isDanger());
    }

    @Test
    public void isWarning_onWarningLevel_returnsTrue() {
        GasStatus status = new GasStatus(GasStatus.LEVEL_WARNING, 400, 0L, "");
        assertFalse(status.isNormal());
        assertTrue(status.isWarning());
        assertFalse(status.isDanger());
    }

    @Test
    public void isDanger_onDangerLevel_returnsTrue() {
        GasStatus status = new GasStatus(GasStatus.LEVEL_DANGER, 900, 0L, "");
        assertFalse(status.isNormal());
        assertFalse(status.isWarning());
        assertTrue(status.isDanger());
    }

    @Test
    public void getters_returnConstructorValues() {
        long ts = 1700000000000L;
        GasStatus status = new GasStatus(GasStatus.LEVEL_WARNING, 500, ts, "caution");
        assertEquals(GasStatus.LEVEL_WARNING, status.getLevel());
        assertEquals(500, status.getConcentration());
        assertEquals(ts, status.getTimestamp());
        assertEquals("caution", status.getMessage());
    }

    @Test
    public void thresholdConstants_haveExpectedValues() {
        assertEquals(300, GasStatus.WARNING_THRESHOLD);
        assertEquals(800, GasStatus.DANGER_THRESHOLD);
    }
}
