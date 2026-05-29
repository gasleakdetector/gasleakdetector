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
        runWithTimingDisabled {
            // object created outside measurement
        }
        s.isNormal
        s.isWarning
        s.isDanger
    }
}
