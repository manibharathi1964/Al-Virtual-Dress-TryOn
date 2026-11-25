package com.mj.virtualdress

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

data class SelectedImage(
    val previewModel: Any,
    val dataUri: String
)

fun Context.readImageAsDataUri(uri: Uri): SelectedImage? {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val mime = contentResolver.getType(uri) ?: "image/jpeg"
    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return SelectedImage(uri, "data:$mime;base64,$encoded")
}

fun bitmapToSelectedImage(bitmap: Bitmap): SelectedImage {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
    val encoded = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    return SelectedImage(bitmap, "data:image/jpeg;base64,$encoded")
}
