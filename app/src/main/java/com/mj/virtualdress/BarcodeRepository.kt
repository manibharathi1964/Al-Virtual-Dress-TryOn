package com.mj.virtualdress

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object BarcodeRepository {
    suspend fun scan(bitmap: Bitmap): String? {
        val scanner: BarcodeScanner = BarcodeScanning.getClient()
        return try {
            val result = await(scanner.process(InputImage.fromBitmap(bitmap, 0)))
            result.firstOrNull()?.rawValue
        } finally {
            scanner.close()
        }
    }

    private suspend fun <T> await(task: com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }
}
