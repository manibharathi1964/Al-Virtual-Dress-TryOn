package com.mj.virtualdress

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PersonAnalysis(
    val quality: InputQualityReport,
    val pose: PoseValidation,
    val foregroundRatio: Float
) {
    val accepted: Boolean
        get() = quality.isAcceptable && pose.accepted && foregroundRatio >= 0.08f
}

object PersonInputAnalyzer {
    suspend fun analyze(bitmap: Bitmap): PersonAnalysis {
        val quality = InputQualityAnalyzer.analyze(bitmap)
        if (!quality.isAcceptable) {
            return PersonAnalysis(quality, PoseValidation(false, quality.message), 0f)
        }

        val image = InputImage.fromBitmap(bitmap, 0)
        val poseDetector = PoseDetection.getClient(
            AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
        )
        val pose = awaitPose(poseDetector.process(image))
        poseDetector.close()

        val poseLandmarks = pose.allPoseLandmarks
        val poseAccepted = poseLandmarks.size >= 8 &&
            poseLandmarks.count { it.inFrameLikelihood >= 0.5f } >= 6
        val poseMessage = if (poseAccepted) {
            "Person and body pose detected."
        } else {
            "Keep the full person visible, preferably standing upright."
        }

        val segmenter = Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
        val mask = awaitSegmentation(segmenter.process(image))
        segmenter.close()

        val buffer = mask.buffer
        var foreground = 0
        var total = 0
        while (buffer.hasRemaining()) {
            if (buffer.float > 0.5f) foreground++
            total++
        }
        val foregroundRatio = if (total == 0) 0f else foreground.toFloat() / total

        return PersonAnalysis(
            quality,
            PoseValidation(poseAccepted, poseMessage),
            foregroundRatio
        )
    }

    private suspend fun <T> awaitPose(task: com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }

    private suspend fun <T> awaitSegmentation(task: com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }
}
