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
        p.id        = 1L
        p.deviceId  = "device-01"
        p.gasPpm    = 450
        p.status    = "warning"
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

    @Test
    fun construct_rawPoint_10k() = benchmarkRule.measureRepeated {
        repeat(10_000) { i ->
            HistoricalDataPoint(i % 1200, "2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun construct_fullFields_5000() = benchmarkRule.measureRepeated {
        repeat(5_000) { i ->
            HistoricalDataPoint().apply {
                id        = i.toLong()
                deviceId  = "device-${i % 10}"
                gasPpm    = i % 1200
                status    = if (i % 3 == 0) "normal" else if (i % 3 == 1) "warning" else "danger"
                ipAddress = "192.168.${i % 256}.${(i / 256) % 256}"
                createdAt = "2024-06-15T${(i % 24).toString().padStart(2,'0')}:00:00.000Z"
            }
        }
    }

    @Test
    fun getGasPpm_mixedPath_50k() = benchmarkRule.measureRepeated {
        val raw = runWithTimingDisabled { makePoint(450) }
        val avg = runWithTimingDisabled { HistoricalDataPoint().also { it.avgGas = 512.5f } }
        repeat(50_000) { i ->
            if (i % 2 == 0) raw.gasPpm else avg.gasPpm
        }
    }

    @Test
    fun parseTimestamp_10k_multipleFormats() = benchmarkRule.measureRepeated {
        val timestamps = listOf(
            "2024-01-01T00:00:00Z",
            "2024-06-15T10:30:00.000Z",
            "2023-12-31T23:59:59.999Z",
            "2024-03-20T06:00:00.123Z",
            "2024-09-05T18:45:30.500Z"
        )
        repeat(10_000) { i ->
            val p = runWithTimingDisabled {
                makePoint(createdAt = timestamps[i % timestamps.size])
            }
            p.parsedTimestampMs
        }
    }

    @Test
    fun createAndSort_10000Points() = benchmarkRule.measureRepeated {
        val list = (0 until 10_000).map { i ->
            HistoricalDataPoint(i % 1200, "2024-01-01T${(i % 24).toString().padStart(2,'0')}:00:00Z")
        }
        list.sortedBy { it.parsedTimestampMs }
    }

    @Test
    fun setBucket_20k() = benchmarkRule.measureRepeated {
        val p = runWithTimingDisabled { HistoricalDataPoint() }
        repeat(20_000) {
            p.bucket = "2024-01-01T00:00:00Z"
        }
    }

    @Test
    fun getCreatedAt_bothPaths_20k() = benchmarkRule.measureRepeated {
        val direct = runWithTimingDisabled { makePoint() }
        val viaSet = runWithTimingDisabled { HistoricalDataPoint().also { it.bucket = "2024-07-01T00:00:00Z" } }
        repeat(20_000) { i ->
            if (i % 2 == 0) direct.createdAt else viaSet.createdAt
        }
    }
}
