package adapter

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

private val logger = KotlinLogging.logger {}

interface ScanAdapter {
    suspend fun scan(domain: String, scanId: String)

    fun sendRequest(requestBody: RequestBody, webhookEndpoint: String) {
        logger.info { "Preparing to send request to webhook endpoint: $webhookEndpoint" }
        val client = OkHttpClient()
        val request = Request.Builder().url(webhookEndpoint).post(requestBody).build()
        logger.debug { "Request built for $webhookEndpoint with body: $requestBody" }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logger.error(e) { "Failed to send request to $webhookEndpoint: ${e.message}" }
            }

            override fun onResponse(call: Call, response: Response) {
                logger.info { "Received response from $webhookEndpoint, isSuccessful=${response.isSuccessful}, code=${response.code}" }
                response.close()
            }
        })
        logger.info { "Request sent asynchronously to $webhookEndpoint" }
    }
}