package listener

import adapter.parseAmassFileGrouped
import adapter.parseHarvesterFile
import adapter.waitForFileComplete
import factory.toolFactories
import io.github.oshai.kotlinlogging.KotlinLogging
import repo.ScanDto
import repo.ScanRepo
import repo.ScanResult
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds

private val logger = KotlinLogging.logger {}
private val scanRepo: ScanRepo = ScanRepo()

suspend fun Path.listenTo(targetDomain: String) {
    val watchService = FileSystems.getDefault().newWatchService()
    val path = this

    path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE)
    logger.info { "Started watching directory: $path for new files..." }

    var finishedTools = 0

    while (finishedTools < toolFactories.size) {
        val key = watchService.take()
        logger.debug { "Received watch key event" }
        key.pollEvents()
            .filter { it.kind() == StandardWatchEventKinds.ENTRY_CREATE }
            .forEach { event ->
                val filename = event.context() as Path
                val fullPath = path.resolve(filename)
                logger.info { "New file detected: $filename at $fullPath" }

                try {
                    logger.info { "Handling new file: $fullPath" }
                    val scanResults = handleNewFile(fullPath)
                    logger.info { "Parsed scan results: ${scanResults?.size ?: 0} entries from $fullPath" }
                    scanRepo.saveScanResult(
                        ScanDto(
                            results = scanResults,
                            fullPath = fullPath,
                            targetDomain = targetDomain
                        )
                    )
                    logger.info { "Saved scan results for $fullPath" }
                    if (!fullPath.fileName.toString().endsWith(".xml")) finishedTools++
                    logger.info { "Incremented finishedTools: $finishedTools/${toolFactories.size}" }
                } catch (e: Exception) {
                    logger.error(e) { "Error handling file $fullPath: ${e.message}" }
                }
            }
        key.reset()
    }
    logger.info { "Finished watching directory: $path. Expected number of tools ($finishedTools) completed." }
}

public suspend fun handleNewFile(fullPath: Path): List<ScanResult>? {
    val fileName = fullPath.fileName.toString()
    logger.info { "Determining file type for $fileName" }
    return when {
        fileName.startsWith("amass_") && fileName.endsWith(".txt") -> {
            logger.info { "Detected Amass file: $fileName. Waiting for file to be fully written..." }
            if (waitForFileComplete(fullPath)) {
                logger.info { "Amass file $fileName is fully written. Parsing..." }
                parseAmassFileGrouped(fullPath)
            } else {
                logger.warn { "Timeout waiting for amass file to be fully written: $fullPath" }
                null
            }
        }

        fileName.startsWith("harvester_") && fileName.endsWith(".json") -> {
            logger.info { "Detected Harvester file: $fileName. Parsing..." }
            parseHarvesterFile(fullPath.toFile().readText())
        }

        else -> {
            logger.warn { "Unknown file type or unsupported file: $fileName" }
            null
        }
    }
}