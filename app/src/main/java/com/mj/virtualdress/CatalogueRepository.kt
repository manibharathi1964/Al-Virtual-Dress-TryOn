package com.mj.virtualdress

import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream

object CatalogueRepository {
    private val client = OkHttpClient()

    suspend fun loadProductImage(product: CatalogueProduct): SelectedImage = withContext(Dispatchers.IO) {
        val response = client.newCall(Request.Builder().url(product.imageUrl).build()).execute()
        response.use {
            if (!it.isSuccessful) error("Product image unavailable.")
            val bytes = it.body?.bytes() ?: error("Product image is empty.")
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("Product image could not be decoded.")
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            SelectedImage(
                previewModel = product.imageUrl,
                dataUri = "data:image/jpeg;base64,$encoded",
                bitmap = bitmap
            )
        }
    }
}
