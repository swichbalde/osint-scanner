package adapter

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import repo.ScanResult

private val logger = KotlinLogging.logger {}

class HarvesterAdapter : ScanAdapter {

    private val webhookEndpoint = "http://localhost:9001/hooks/harvester-scan"

    override suspend fun scan(domain: String, scanId: String) {
        logger.info { "Preparing Harvester scan request for domain: $domain, scanId: $scanId" }
        val body = """
                        {
                            "domain": "$domain",
                            "source": "bing",
                            "output_file": "harvester_$scanId"
                        }
                    """.trimIndent()
            .toRequestBody("application/json".toMediaType())
        logger.debug { "Request body for Harvester scan: $body" }
        sendRequest(body, webhookEndpoint)
        logger.info { "Harvester scan request sent for domain: $domain, scanId: $scanId to $webhookEndpoint" }
    }
}

fun parseHarvesterFile(jsonStr: String): List<ScanResult> {
    logger.info { "Parsing Harvester JSON result..." }
    val jsonElement = try {
        Json.parseToJsonElement(jsonStr)
    } catch (e: Exception) {
        logger.error(e) { "Failed to parse JSON string: ${e.message}" }
        return emptyList()
    }

    if (jsonElement !is JsonObject) {
        logger.warn { "Parsed JSON is not a JsonObject. Returning empty list." }
        return emptyList()
    }

    val results = jsonElement.entries
        .filter { it.value is JsonArray }
        .map { entry ->
            logger.debug { "Found array field: '${entry.key}' with ${(entry.value as JsonArray).size} entries" }
            ScanResult(
                type = entry.key,
                value = (entry.value as JsonArray).map { it.jsonPrimitive.content }
            )
        }
    logger.info { "Extracted ${results.size} array fields from Harvester JSON." }
    return results
}

