package com.gasleakdetector.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gasleakdetector.data.local.LocalDataStorage
import com.gasleakdetector.data.model.HistoricalDataPoint
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalDataStorageBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var storage: LocalDataStorage

    private fun makePoint(ppm: Int) = HistoricalDataPoint(ppm, "2024-01-01T00:00:00Z").also {
        it.deviceId = "device-01"
        it.status = if (ppm < 300) "normal" else if (ppm < 800) "warning" else "danger"
        it.ipAddress = "192.168.1.1"
    }

    private fun makeList(n: Int) = (0 until n).map { makePoint(it % 1000) }

    @Before
    fun setup() {
        storage = LocalDataStorage(context)
        storage.clearCache()
    }

    @After
    fun teardown() {
        storage.clearCache()
    }

    @Test
    fun saveNodes_100() = benchmarkRule.measureRepeated {
        val list = runWithTimingDisabled { makeList(100) }
        storage.saveNodes(list)
        runWithTimingDisabled { storage.clearCache() }
    }

    @Test
    fun saveNodes_500() = benchmarkRule.measureRepeated {
        val list = runWithTimingDisabled { makeList(500) }
        storage.saveNodes(list)
        runWithTimingDisabled { storage.clearCache() }
    }

    @Test
    fun saveNodes_1000() = benchmarkRule.measureRepeated {
        val list = runWithTimingDisabled { makeList(1000) }
        storage.saveNodes(list)
        runWithTimingDisabled { storage.clearCache() }
    }

    @Test
    fun saveNodes_overLimit_trimmed() = benchmarkRule.measureRepeated {
        val list = runWithTimingDisabled { makeList(1200) }
        storage.saveNodes(list)
        runWithTimingDisabled { storage.clearCache() }
    }

    @Test
    fun loadNodes_100() = benchmarkRule.measureRepeated {
        runWithTimingDisabled { storage.saveNodes(makeList(100)) }
        storage.loadNodes()
    }

    @Test
    fun loadNodes_500() = benchmarkRule.measureRepeated {
        runWithTimingDisabled { storage.saveNodes(makeList(500)) }
        storage.loadNodes()
    }

    @Test
    fun loadNodes_1000() = benchmarkRule.measureRepeated {
        runWithTimingDisabled { storage.saveNodes(makeList(1000)) }
        storage.loadNodes()
    }

    @Test
    fun addNode_singlePoint() = benchmarkRule.measureRepeated {
        runWithTimingDisabled { storage.saveNodes(makeList(50)) }
        storage.addNode(makePoint(450))
        runWithTimingDisabled { storage.clearCache() }
    }

    @Test
    fun loadNodes_emptyCache() = benchmarkRule.measureRepeated {
        runWithTimingDisabled { storage.clearCache() }
        storage.loadNodes()
    }

    @Test
    fun hasCache_withData() = benchmarkRule.measureRepeated {
        runWithTimingDisabled { storage.saveNodes(makeList(10)) }
        storage.hasCache()
    }

    @Test
    fun hasCache_emptyCache() = benchmarkRule.measureRepeated {
        runWithTimingDisabled { storage.clearCache() }
        storage.hasCache()
    }
}
