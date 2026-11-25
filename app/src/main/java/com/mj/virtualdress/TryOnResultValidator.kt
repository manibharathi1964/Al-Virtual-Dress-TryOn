package com.mj.virtualdress

import android.graphics.BitmapFactory
import android.util.Base64
import kotlin.math.abs

data class TryOnResultQuality(
    val accepted: Boolean,
    val message: String,
    val width: Int,
    val height: Int,
    val detailScore: Double
)

object TryOnResultValidator {
    fun validate(dataUri: String): TryOnResultQuality {
        val comma = dataUri.indexOf(',')
        if (comma < 0) return TryOnResultQuality(false, "AI returned an invalid image.", 0, 0, 0.0)
        val bytes = runCatching { Base64.decode(dataUri.substring(comma + 1), Base64.DEFAULT) }.getOrNull()
            ?: return TryOnResultQuality(false, "AI returned an unreadable image.", 0, 0, 0.0)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return TryOnResultQuality(false, "AI returned an unreadable image.", 0, 0, 0.0)
        if (bitmap.width < 512 || bitmap.height < 512) {
            return TryOnResultQuality(false, "AI result resolution is too low. Please retry.", bitmap.width, bitmap.height, 0.0)
        }
        val stepX = (bitmap.width / 40).coerceAtLeast(1)
        val stepY = (bitmap.height / 40).coerceAtLeast(1)
        var count = 0
        var detail = 0.0
        var previous = 0.0
        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                val c = bitmap.getPixel(x, y)
                val l = 0.299 * ((c shr 16) and 0xff) + 0.587 * ((c shr 8) and 0xff) + 0.114 * (c and 0xff)
                if (count > 0) detail += abs(l - previous)
                previous = l
                count++
            }
        }
        val score = if (count == 0) 0.0 else detail / count
        return if (score >= 3.0) {
            TryOnResultQuality(true, "Try-on result quality check passed.", bitmap.width, bitmap.height, score)
        } else {
            TryOnResultQuality(false, "AI result looks too flat or unclear. Please retry.", bitmap.width, bitmap.height, score)
        }
    }
}
