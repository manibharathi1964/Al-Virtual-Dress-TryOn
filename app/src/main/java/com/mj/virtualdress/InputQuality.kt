package com.mj.virtualdress

import android.graphics.Bitmap
import kotlin.math.abs

data class InputQualityReport(
    val isAcceptable: Boolean,
    val message: String,
    val brightness: Float,
    val sharpness: Double,
    val coverage: Float
)

object InputQualityAnalyzer {
    fun analyze(bitmap: Bitmap): InputQualityReport {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 512 || height < 512) {
            return InputQualityReport(false, "Use a larger, clearer photo (minimum 512 × 512).", 0f, 0.0, 0f)
        }

        val sampleStep = (width * height / 10000).coerceAtLeast(1)
        var samples = 0
        var luminanceSum = 0.0
        var edgeSum = 0.0
        var previous = 0.0

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val c = bitmap.getPixel(x, y)
                val l = (0.299 * ((c shr 16) and 0xff) + 0.587 * ((c shr 8) and 0xff) + 0.114 * (c and 0xff))
                luminanceSum += l
                if (samples > 0) edgeSum += abs(l - previous)
                previous = l
                samples++
                x += sampleStep
            }
            y += sampleStep
        }

        val brightness = (luminanceSum / samples / 255.0).toFloat()
        val sharpness = edgeSum / samples
        val goodLight = brightness in 0.18f..0.88f
        val goodDetail = sharpness >= 8.0
        val coverage = 1f

        return if (goodLight && goodDetail) {
            InputQualityReport(true, "Photo quality looks good.", brightness, sharpness, coverage)
        } else {
            val reason = when {
                !goodLight -> "Use a photo with better, even lighting."
                else -> "Use a sharper, less blurry photo."
            }
            InputQualityReport(false, reason, brightness, sharpness, coverage)
        }
    }
}
