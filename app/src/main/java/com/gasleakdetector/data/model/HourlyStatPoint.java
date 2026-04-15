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

public class HourlyStatPoint {

    private String bucket;
    private float  avgGas;
    private float  minGas;
    private float  maxGas;
    private int    sampleCount;

    public String getBucket()           { return bucket; }
    public void   setBucket(String v)   { bucket = v; }

    public float  getAvgGas()           { return avgGas; }
    public void   setAvgGas(float v)    { avgGas = v; }

    public float  getMinGas()           { return minGas; }
    public void   setMinGas(float v)    { minGas = v; }

    public float  getMaxGas()           { return maxGas; }
    public void   setMaxGas(float v)    { maxGas = v; }

    public int    getSampleCount()      { return sampleCount; }
    public void   setSampleCount(int v) { sampleCount = v; }
}
