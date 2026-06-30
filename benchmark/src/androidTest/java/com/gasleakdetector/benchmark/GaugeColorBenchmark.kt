package com.gasleakdetector.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gasleakdetector.data.model.GasStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GaugeColorBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val colorNormal  = 0xFF4CAF50.toInt()
    private val colorWarning = 0xFFFFC107.toInt()
    private val colorDanger  = 0xFFF44336.toInt()

    private fun blendColors(start: Int, end: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        val red   = ((start shr 16 and 0xFF) + ((end shr 16 and 0xFF) - (start shr 16 and 0xFF)) * r).toInt()
        val green = ((start shr  8 and 0xFF) + ((end shr  8 and 0xFF) - (start shr  8 and 0xFF)) * r).toInt()
        val blue  = ((start        and 0xFF) + ((end        and 0xFF) - (start        and 0xFF)) * r).toInt()
        return 0xFF000000.toInt() or (red shl 16) or (green shl 8) or blue
    }

    private fun colorForValue(ppm: Int): Int {
        val warn   = GasStatus.WARNING_THRESHOLD
        val danger = GasStatus.DANGER_THRESHOLD
        return when {
            ppm < warn   -> colorNormal
            ppm < danger -> {
                val ratio = (ppm - warn) / (danger - warn).toFloat()
                blendColors(colorNormal, colorWarning, ratio)
            }
            else -> {
                val ratio = ((ppm - danger) / 200f).coerceAtMost(1f)
                blendColors(colorWarning, colorDanger, ratio)
            }
        }
    }

    @Test
    fun colorForValue_normal_zone() = benchmarkRule.measureRepeated {
        colorForValue(150)
    }

    @Test
    fun colorForValue_warning_zone() = benchmarkRule.measureRepeated {
        colorForValue(550)
    }

    @Test
    fun colorForValue_danger_zone() = benchmarkRule.measureRepeated {
        colorForValue(900)
    }

    @Test
    fun blendColors_single() = benchmarkRule.measureRepeated {
        blendColors(colorNormal, colorWarning, 0.5f)
    }

    @Test
    fun colorForValue_sweep_0_to_1000() = benchmarkRule.measureRepeated {
        for (ppm in 0..1000 step 10) {
            colorForValue(ppm)
        }
    }

    @Test
    fun colorForValue_fullSweep_step1() = benchmarkRule.measureRepeated {
        for (ppm in 0..1999) {
            colorForValue(ppm)
        }
    }

    @Test
    fun colorForValue_uiFrameStress_50k() = benchmarkRule.measureRepeated {
        val values = intArrayOf(0, 150, 299, 300, 450, 550, 799, 800, 900, 1000,
                                1100, 1200, 1500, 1800, 1999)
        repeat(50_000) { i ->
            colorForValue(values[i % values.size])
        }
    }

    @Test
    fun blendColors_100k_varyingRatio() = benchmarkRule.measureRepeated {
        repeat(100_000) { i ->
            blendColors(colorNormal, colorDanger, i / 100_000f)
        }
    }

    @Test
    fun colorForValue_60fps_30s_simulation() = benchmarkRule.measureRepeated {
        repeat(1_800) { frame ->
            for (point in 0 until 100) {
                colorForValue((frame * 100 + point) % 1200)
            }
        }
    }

    @Test
    fun blendColors_allPairs_10k_each() = benchmarkRule.measureRepeated {
        repeat(10_000) { i ->
            val r = i / 10_000f
            blendColors(colorNormal,  colorWarning, r)
            blendColors(colorWarning, colorDanger,  r)
            blendColors(colorNormal,  colorDanger,  r)
        }
    }

    @Test
    fun colorForValue_chartBatch_5000Points() = benchmarkRule.measureRepeated {
        val points = runWithTimingDisabled {
            IntArray(5_000) { it % 1200 }
        }
        for (ppm in points) colorForValue(ppm)
    }
}
