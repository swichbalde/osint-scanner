package orchestrator

import factory.toolFactories
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import listener.listenTo
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger {}

class ScanOrchestrator {

    fun runScan(domain: String) {
        val scanId = UUID.randomUUID().toString()
        logger.info { "Starting scan $scanId for $domain" }

        runBlocking {
            val outputDir = File("/data/scan-results")
            if (outputDir.mkdirs()) {
                logger.info { "Created output directory: ${outputDir.absolutePath}" }
            } else {
                logger.info { "Output directory already exists: ${outputDir.absolutePath}" }
            }
            val outputPath = outputDir.toPath()

            logger.info { "Launching directory watcher on $outputPath" }
            launch(Dispatchers.IO) {
                outputPath.listenTo(domain)
            }

            val results = toolFactories.map { factory ->
                launch {
                    logger.info { "Starting tool: ${factory.name} for domain: $domain, scanId: $scanId" }
                    try {
                        factory.adapter.scan(domain, scanId)
                    } catch (e: Exception) {
                        logger.error(e) { "Error running tool ${factory.name}: ${e.message}" }
                    }
                }
            }
            results.forEach { it.join() }
        }
    }
}
