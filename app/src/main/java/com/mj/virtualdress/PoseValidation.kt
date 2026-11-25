package com.mj.virtualdress

data class PoseValidation(
    val accepted: Boolean,
    val message: String
)

/*
 * P2 validation boundary.
 * The production detector is intentionally isolated here so ML Kit pose/segmentation
 * can be upgraded without changing the try-on API or UI flow.
 */
object PoseValidator {
    fun validatePersonPhoto(width: Int, height: Int): PoseValidation {
        if (width < 512 || height < 512) {
            return PoseValidation(false, "Person photo is too small for reliable pose analysis.")
        }
        val ratio = height.toFloat() / width.toFloat()
        return if (ratio >= 0.75f) {
            PoseValidation(true, "Portrait framing is suitable for try-on.")
        } else {
            PoseValidation(false, "Use a portrait-oriented photo showing the person clearly.")
        }
    }
}
