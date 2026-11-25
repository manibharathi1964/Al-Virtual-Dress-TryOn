package com.mj.virtualdress

import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class TryOnRequest(
    val modelImage: String,
    val garmentImage: String,
    val category: String = "auto",
    val mode: String = "balanced",
    val instruction: String = ""
)

class TryOnApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generate(request: TryOnRequest): String {
        val payload = JSONObject()
            .put("model_image", request.modelImage)
            .put("garment_image", request.garmentImage)
            .put("category", request.category)
            .put("mode", request.mode)
            .put("instruction", request.instruction)

        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val baseUrl = BuildConfig.VTO_API_BASE_URL.trimEnd('/')
        if (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://10.0.2.2") && !baseUrl.startsWith("http://localhost")) {
            throw IOException("Secure HTTPS AI server URL is required.")
        }
        val url = baseUrl + "/v1/tryon"

        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(httpRequest).execute()
        response.use {
            val text = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrDefault("")
                throw IOException(message.ifBlank { "Try-on service returned HTTP " + it.code })
            }

            val image = JSONObject(text).optString("image_data_uri")
            if (image.isBlank()) throw IOException("Try-on service returned no result image.")
            val quality = TryOnResultValidator.validate(image)
            if (!quality.accepted) throw IOException(quality.message)
            return image
        }
    }
}
