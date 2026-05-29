package com.gasleakdetector.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gasleakdetector.data.model.HistoricalDataPoint
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoricalDataPointBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private fun makePoint(ppm: Int = 450, createdAt: String = "2024-01-01T00:00:00Z") =
        HistoricalDataPoint(ppm, createdAt)

    @Test
    fun construct_rawPoint() = benchmarkRule.measureRepeated {
        HistoricalDataPoint(450, "2024-01-01T00:00:00Z")
    }

    @Test
    fun construct_emptyAndSetFields() = benchmarkRule.measureRepeated {
        val p = HistoricalDataPoint()
        p.id = 1L
        p.deviceId = "device-01"
        p.gasPpm = 450
        p.status = "warning"
        p.ipAddress = "192.168.1.1"
        p.createdAt = "2024-01-01T00:00:00Z"
    }

    @Test
    fun getGasPpm_rawField() = benchmarkRule.measureRepeated {
        val p = runWithTimingDisabled { makePoint(450) }
        p.gasPpm
    }

    @Test
    fun getGasPpm_fallbackToAvgGas() = benchmarkRule.measureRepeated {
        val p = runWithTimingDisabled {
            HistoricalDataPoint().also { it.avgGas = 512.5f }
        }
        p.gasPpm
    }

    @Test
    fun getCreatedAt_fromCreatedAt() = benchmarkRule.measureRepeated {
        val p = runWithTimingDisabled { makePoint() }
        p.createdAt
    }

    @Test
    fun getCreatedAt_fallbackToBucket() = benchmarkRule.measureRepeated {
        val p = runWithTimingDisabled {
            HistoricalDataPoint().also { it.bucket = "2024-01-01T00:00:00Z" }
        }
        p.createdAt
    }

    @Test
    fun setBucket_setsCreatedAt() = benchmarkRule.measureRepeated {
        val p = runWithTimingDisabled { HistoricalDataPoint() }
        p.bucket = "2024-01-01T00:00:00Z"
    }

    @Test
    fun parseTimestamp() = benchmarkRule.measureRepeated {
        val p = runWithTimingDisabled { makePoint(createdAt = "2024-06-15T10:30:00.000Z") }
        p.parsedTimestampMs
    }

    @Test
    fun createList_1000Points() = benchmarkRule.measureRepeated {
        val list = runWithTimingDisabled {
            (0 until 1000).map { makePoint(it % 1000) }
        }
        list.size
    }
}
