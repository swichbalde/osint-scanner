package adapter

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import repo.ScanResult
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class AmassAdapter : ScanAdapter {

    private val webhookEndpoint = "http://amass:9000/hooks/amass-scan"

    override suspend fun scan(domain: String, scanId: String) {
        logger.info { "Preparing Amass scan request for domain: $domain, scanId: $scanId" }
        val body = """
            {
                "domain": "$domain",
                "output_file": "amass_${scanId}.txt"
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())
        logger.debug { "Request body for Amass scan: $body" }
        sendRequest(body, webhookEndpoint)
        logger.info { "Amass scan request sent for domain: $domain, scanId: $scanId to $webhookEndpoint" }
    }
}

suspend fun waitForFileComplete(
    path: Path,
    stableDurationSeconds: Long = 1,
    maxTotalSeconds: Long = 40,
): Boolean {
    var lastSize = -1L
    var stableSeconds = 0L
    var waitedSeconds = 0L

    val backoffDelays = listOf(3L, 9L, 27L)
    var attempt = 0

    logger.info { "Waiting for file size to stabilize for $path" }

    while (waitedSeconds < maxTotalSeconds) {
        val currentSize = try {
            path.toFile().length()
        } catch (e: Exception) {
            logger.warn { "Exception reading file size for $path: ${e.message}" }
            0L
        }
        logger.debug { "File size for $path: $currentSize bytes (lastSize: $lastSize)" }
        if (currentSize == lastSize && currentSize > 0) {
            stableSeconds++
            logger.debug { "File size unchanged for $stableSeconds seconds" }
            if (stableSeconds >= stableDurationSeconds) {
                logger.info { "File size stabilized for $stableDurationSeconds seconds. Proceeding with $path" }
                return true
            }
        } else {
            stableSeconds = 0
        }
        lastSize = currentSize

        val delaySeconds = if (attempt < backoffDelays.size) backoffDelays[attempt] else backoffDelays.last()
        logger.info { "File not stable yet, file size: ${path.toFile().length()}, stable seconds: $stableSeconds. Retry #${attempt + 1} in $delaySeconds seconds..." }
        delay(delaySeconds * 1000)
        waitedSeconds += delaySeconds
        attempt++
    }
    logger.warn { "File size did not stabilize within $maxTotalSeconds seconds for $path" }
    return false
}


fun parseAmassFileGrouped(file: Path): List<ScanResult> {
    logger.info { "Parsing Amass file: $file" }
    val map = mutableMapOf<String, MutableList<String>>()
    var lineCount = 0
    file.toFile().forEachLine { line ->
        lineCount++
        val type = extractType(line)
        if (type != null) {
            logger.debug { "Extracted type '$type' from line: $line" }
            map.getOrPut(type) { mutableListOf() }.add(line)
        } else {
            logger.debug { "Could not extract type from line: $line" }
        }
    }
    logger.info { "Parsed $lineCount lines from $file; found ${map.size} type groups." }
    if (map.isEmpty()) {
        logger.warn { "No valid data found in Amass file: $file" }
        return emptyList()
    }
    return map.map { (type, values) ->
        logger.debug { "Creating ScanResult for type: $type with ${values.size} entries" }
        ScanResult(type, values)
    }
}

private fun extractType(line: String): String? {
    val generalRegex = Regex(""".+\(([^)]+)\)\s+-->\s+.+?\s+-->\s+.+\(([^)]+)\)""")
    val match = generalRegex.find(line)
    return if (match != null) {
        val (leftType, rightType) = match.destructured
        "$leftType -> $rightType"
    } else {
        null
    }
}