package com.voicelike.app

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val campfirePaymentTag = "CampfirePayment"

internal data class CampfireCreateOrderRequest(
    val sponsorName: String?,
    val language: String?
)

internal data class CampfireCreateOrderResponse(
    val orderId: String,
    val amount: String,
    val subject: String,
    val checkoutUrl: String
)

internal data class CampfirePaymentStatusResponse(
    val orderId: String,
    val amount: String,
    val status: String,
    val paid: Boolean,
    val paidAt: String,
    val sponsorName: String,
    val updatedAt: String
)

internal data class CampfirePaymentReturnEvent(
    val orderId: String?,
    val status: String?,
    val tradeNo: String?,
    val uri: Uri
)

internal object CampfirePaymentReturnCenter {
    private val _events = MutableSharedFlow<CampfirePaymentReturnEvent>(replay = 1, extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "photoo" || data.host != "campfire-pay") return
        _events.tryEmit(
            CampfirePaymentReturnEvent(
                orderId = data.getQueryParameter("orderId"),
                status = data.getQueryParameter("status"),
                tradeNo = data.getQueryParameter("tradeNo"),
                uri = data
            )
        )
    }
}

internal object CampfirePaymentApi {
    private val gson = Gson()

    fun isConfigured(): Boolean {
        return BuildConfig.CAMPFIRE_PAY_BASE_URL.isNotBlank()
    }

    suspend fun createOrder(
        sponsorName: String?,
        language: String?
    ): CampfireCreateOrderResponse = withContext(Dispatchers.IO) {
        val payload = gson.toJson(
            CampfireCreateOrderRequest(
                sponsorName = sponsorName?.takeIf { it.isNotBlank() },
                language = language?.takeIf { it.isNotBlank() }
            )
        )
        requestJson(
            method = "POST",
            path = "/api/campfire/payments",
            requestBody = payload,
            responseClass = CampfireCreateOrderResponse::class.java
        )
    }

    suspend fun queryStatus(orderId: String): CampfirePaymentStatusResponse = withContext(Dispatchers.IO) {
        requestJson(
            method = "GET",
            path = "/api/campfire/payments/${Uri.encode(orderId)}/status",
            requestBody = null,
            responseClass = CampfirePaymentStatusResponse::class.java
        )
    }

    private fun <T> requestJson(
        method: String,
        path: String,
        requestBody: String?,
        responseClass: Class<T>
    ): T {
        val baseUrl = BuildConfig.CAMPFIRE_PAY_BASE_URL.trim().removeSuffix("/")
        require(baseUrl.isNotBlank()) { "Campfire payment base URL is not configured." }

        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
        }

        if (requestBody != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
            }
        }

        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.use {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText()
        }.orEmpty()

        if (statusCode !in 200..299) {
            Log.e(campfirePaymentTag, "HTTP $statusCode: $body")
            throw IllegalStateException(body.ifBlank { "Campfire payment request failed: HTTP $statusCode" })
        }

        return gson.fromJson(body, responseClass)
    }
}
