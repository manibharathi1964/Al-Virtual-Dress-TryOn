package com.mj.virtualdress

import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class TryOnResponse(
    val imageDataUri: String,
    val resultUrl: String?
)

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
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generate(request: TryOnRequest): TryOnResponse {
        val baseUrl = BuildConfig.VTO_API_BASE_URL.trimEnd('/')
        return if (baseUrl.contains("hf.space", ignoreCase = true)) {
            generateWithFreeHuggingFaceSpace(baseUrl, request)
        } else {
            generateWithCompatibleServer(baseUrl, request)
        }
    }

    private fun generateWithFreeHuggingFaceSpace(
        baseUrl: String,
        request: TryOnRequest
    ): TryOnResponse {
        val personBase64 = stripDataUri(request.modelImage)
        val garmentBase64 = stripDataUri(request.garmentImage)
        val clothType = when (request.category.lowercase()) {
            "lower", "pants", "jeans", "trousers", "skirt" -> "lower"
            "overall", "dress", "jumpsuit", "kurti" -> "overall"
            else -> "upper"
        }

        val payload = JSONObject()
            .put("data", org.json.JSONArray()
                .put(personBase64)
                .put(garmentBase64)
                .put(clothType))
            .toString()

        val startRequest = Request.Builder()
            .url(baseUrl + "/gradio_api/call/try_on")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val startResponse = client.newCall(startRequest).execute()
        val eventId = startResponse.use {
            val text = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                throw IOException("Free AI try-on service returned HTTP ${it.code}")
            }
            JSONObject(text).optString("event_id").ifBlank {
                throw IOException("Free AI service did not return a job id.")
            }
        }

        val resultRequest = Request.Builder()
            .url(baseUrl + "/gradio_api/call/try_on/" + eventId)
            .get()
            .build()

        val resultResponse = client.newCall(resultRequest).execute()
        val output = resultResponse.use {
            val text = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                throw IOException("Free AI try-on result request returned HTTP ${it.code}")
            }
            parseGradioResult(text)
        }

        val json = runCatching { JSONObject(output) }.getOrElse {
            throw IOException("Free AI service returned an invalid result.")
        }
        val outputBase64 = json.optString("output")
        if (outputBase64.isBlank()) {
            val error = json.optString("error")
            throw IOException(error.ifBlank { "Free AI service returned no result image." })
        }

        val imageDataUri = if (outputBase64.startsWith("data:image", ignoreCase = true)) {
            outputBase64
        } else {
            "data:image/jpeg;base64,$outputBase64"
        }

        val quality = TryOnResultValidator.validate(imageDataUri)
        if (!quality.accepted) throw IOException(quality.message)

        return TryOnResponse(imageDataUri, null)
    }

    private fun generateWithCompatibleServer(
        baseUrl: String,
        request: TryOnRequest
    ): TryOnResponse {
        if (!baseUrl.startsWith("https://") &&
            !baseUrl.startsWith("http://10.0.2.2") &&
            !baseUrl.startsWith("http://localhost")
        ) {
            throw IOException("Secure HTTPS AI server URL is required.")
        }

        val payload = JSONObject()
            .put("model_image", request.modelImage)
            .put("garment_image", request.garmentImage)
            .put("category", request.category)
            .put("mode", request.mode)
            .put("instruction", request.instruction)

        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(baseUrl + "/v1/tryon")
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
            return TryOnResponse(image, JSONObject(text).optString("result_url").ifBlank { null })
        }
    }

    private fun parseGradioResult(sse: String): String {
        val dataLines = sse.lineSequence()
            .filter { it.startsWith("data:") }
            .map { it.removePrefix("data:").trim() }
            .filter { it.isNotBlank() }
            .toList()

        val last = dataLines.lastOrNull()
            ?: throw IOException("Free AI service returned no event data.")

        val parsed = runCatching { JSONObject(last) }.getOrNull()
        if (parsed != null) {
            if (parsed.has("output") || parsed.has("error")) return parsed.toString()
            if (parsed.has("data")) {
                val data = parsed.optJSONArray("data")
                val first = data?.opt(0)
                if (first is String) return first
            }
        }

        val array = runCatching { org.json.JSONArray(last) }.getOrNull()
        if (array != null && array.length() > 0) {
            val first = array.opt(0)
            if (first is String) return first
            if (first is JSONObject) return first.toString()
        }

        return last
    }

    private fun stripDataUri(value: String): String {
        val comma = value.indexOf(',')
        return if (value.startsWith("data:", ignoreCase = true) && comma >= 0) {
            value.substring(comma + 1)
        } else {
            value
        }
    }
}
