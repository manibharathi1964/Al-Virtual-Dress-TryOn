package com.mj.virtualdress

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class GarmentAnalysis(
    val validation: GarmentValidation,
    val category: String,
    val color: String,
    val isolatedBitmap: Bitmap
)

object GarmentAnalyzer {
    suspend fun analyze(bitmap: Bitmap): GarmentAnalysis {
        val validation = GarmentValidator.validate(bitmap)
        if (!validation.accepted) {
            return GarmentAnalysis(validation, "Unknown", "Unknown", bitmap)
        }

        val labels = runCatching {
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            val result = awaitLabels(labeler.process(InputImage.fromBitmap(bitmap, 0)))
            labeler.close()
            result
        }.getOrDefault(emptyList())

        val category = labels
            .sortedByDescending { it.confidence }
            .firstNotNullOfOrNull { label ->
                when (label.text.lowercase()) {
                    "shirt", "t-shirt", "tshirt", "top", "blouse", "dress", "jacket", "coat",
                    "jeans", "pants", "trousers", "skirt", "shorts" -> label.text
                    "clothing", "fashion accessory" -> "Clothing"
                    else -> null
                }
            } ?: "Clothing"

        return GarmentAnalysis(
            validation = validation,
            category = category,
            color = dominantColor(bitmap),
            isolatedBitmap = isolateBackground(bitmap)
        )
    }

    private fun dominantColor(bitmap: Bitmap): String {
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0L
        val stepX = (bitmap.width / 40).coerceAtLeast(1)
        val stepY = (bitmap.height / 40).coerceAtLeast(1)
        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                val c = bitmap.getPixel(x, y)
                val max = maxOf(Color.red(c), Color.green(c), Color.blue(c))
                val min = minOf(Color.red(c), Color.green(c), Color.blue(c))
                if (max - min < 12 && max > 235) continue
                r += Color.red(c); g += Color.green(c); b += Color.blue(c); n++
            }
        }
        if (n == 0L) return "Unknown"
        val rr = (r / n).toInt()
        val gg = (g / n).toInt()
        val bb = (b / n).toInt()
        return when {
            maxOf(rr, gg, bb) < 55 -> "Black"
            minOf(rr, gg, bb) > 205 -> "White"
            rr > gg * 1.25 && rr > bb * 1.2 -> "Red"
            bb > rr * 1.2 && bb > gg * 1.05 -> "Blue"
            gg > rr * 1.15 && gg > bb * 1.05 -> "Green"
            rr > 150 && gg > 100 && bb < 90 -> "Yellow"
            rr > 110 && gg < 100 && bb < 100 -> "Brown"
            else -> "Mixed"
        }
    }

    private fun isolateBackground(bitmap: Bitmap): Bitmap {
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val corners = listOf(
            bitmap.getPixel(0, 0),
            bitmap.getPixel(bitmap.width - 1, 0),
            bitmap.getPixel(0, bitmap.height - 1),
            bitmap.getPixel(bitmap.width - 1, bitmap.height - 1)
        )
        val bgR = corners.map(Color::red).average()
        val bgG = corners.map(Color::green).average()
        val bgB = corners.map(Color::blue).average()
        val threshold = 48.0
        for (y in 0 until out.height) {
            for (x in 0 until out.width) {
                val c = out.getPixel(x, y)
                val d = kotlin.math.sqrt(
                    (Color.red(c) - bgR) * (Color.red(c) - bgR) +
                    (Color.green(c) - bgG) * (Color.green(c) - bgG) +
                    (Color.blue(c) - bgB) * (Color.blue(c) - bgB)
                )
                if (d < threshold) out.setPixel(x, y, Color.TRANSPARENT)
            }
        }
        return out
    }

    private suspend fun awaitLabels(
        task: com.google.android.gms.tasks.Task<List<com.google.mlkit.vision.label.ImageLabel>>
    ): List<com.google.mlkit.vision.label.ImageLabel> =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }
}
