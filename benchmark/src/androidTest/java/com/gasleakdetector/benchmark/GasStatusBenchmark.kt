package com.gasleakdetector.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gasleakdetector.data.model.GasStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GasStatusBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun calculateLevel_defaultThresholds() = benchmarkRule.measureRepeated {
        GasStatus.calculateLevel(450)
    }

    @Test
    fun calculateLevel_customThresholds() = benchmarkRule.measureRepeated {
        GasStatus.calculateLevel(450, 300, 800)
    }

    @Test
    fun calculateLevel_boundary_normal() = benchmarkRule.measureRepeated {
        GasStatus.calculateLevel(299)
    }

    @Test
    fun calculateLevel_boundary_warning() = benchmarkRule.measureRepeated {
        GasStatus.calculateLevel(300)
    }

    @Test
    fun calculateLevel_boundary_danger() = benchmarkRule.measureRepeated {
        GasStatus.calculateLevel(800)
    }

    @Test
    fun construct_gasStatus() = benchmarkRule.measureRepeated {
        GasStatus(GasStatus.LEVEL_WARNING, 450, System.currentTimeMillis(), "Warning")
    }

    @Test
    fun isNormal_isDanger_isWarning() = benchmarkRule.measureRepeated {
        val s = GasStatus(GasStatus.LEVEL_DANGER, 900, 0L, "Danger")
        runWithTimingDisabled { }
        s.isNormal
        s.isWarning
        s.isDanger
    }

    @Test
    fun calculateLevel_sweep_10k() = benchmarkRule.measureRepeated {
        repeat(10_000) { i ->
            GasStatus.calculateLevel(i % 1200)
        }
    }

    @Test
    fun construct_batchCreate_5000() = benchmarkRule.measureRepeated {
        val ts = System.currentTimeMillis()
        repeat(5_000) { i ->
            GasStatus(GasStatus.LEVEL_WARNING, 300 + i % 500, ts + i, "Warning")
        }
    }

    @Test
    fun propertyAccess_10k_iterations() = benchmarkRule.measureRepeated {
        val normal  = runWithTimingDisabled { GasStatus(GasStatus.LEVEL_NORMAL,  100, 0L, "Normal")  }
        val warning = runWithTimingDisabled { GasStatus(GasStatus.LEVEL_WARNING, 500, 0L, "Warning") }
        val danger  = runWithTimingDisabled { GasStatus(GasStatus.LEVEL_DANGER,  900, 0L, "Danger")  }
        repeat(10_000) {
            normal.isNormal; normal.isWarning; normal.isDanger
            warning.isNormal; warning.isWarning; warning.isDanger
            danger.isNormal; danger.isWarning; danger.isDanger
        }
    }

    @Test
    fun calculateLevel_1000_customThresholdVariants() = benchmarkRule.measureRepeated {
        repeat(1_000) { i ->
            val warn   = 100 + i % 400
            val danger = warn + 200 + i % 300
            GasStatus.calculateLevel(warn + 50, warn, danger)
        }
    }

    @Test
    fun realtimeLoop_create_and_check_10k() = benchmarkRule.measureRepeated {
        val ts = System.currentTimeMillis()
        repeat(10_000) { i ->
            val ppm = i % 1200
            val status = GasStatus(GasStatus.calculateLevel(ppm), ppm, ts, "")
            status.isDanger
        }
    }
}
