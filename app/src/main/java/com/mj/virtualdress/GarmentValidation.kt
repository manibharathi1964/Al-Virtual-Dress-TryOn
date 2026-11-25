package com.mj.virtualdress

import android.graphics.Bitmap

data class GarmentValidation(
    val accepted: Boolean,
    val message: String
)

object GarmentValidator {
    fun validate(bitmap: Bitmap): GarmentValidation {
        if (bitmap.width < 256 || bitmap.height < 256) {
            return GarmentValidation(false, "Garment image is too small.")
        }
        val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        return if (aspect in 0.35f..3.5f) {
            GarmentValidation(true, "Garment image is suitable for processing.")
        } else {
            GarmentValidation(false, "Use a clear garment image with the complete item visible.")
        }
    }
}
