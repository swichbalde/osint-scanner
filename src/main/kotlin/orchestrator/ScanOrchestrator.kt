package orchestrator

import adapter.AmassAdapter
import adapter.HarvesterAdapter
import adapter.ToolFactory
import repo.ScanResult
import java.util.*

class ScanOrchestrator {
    private val toolFactories = listOf(
        ToolFactory("harvester", HarvesterAdapter()),
        ToolFactory("amass", AmassAdapter())
    )

    fun runScan(domain: String, outputType: String) {
        val scanId = UUID.randomUUID().toString()
//        log("Starting scan $scanId for $domain")

        // Run tools concurrently
        val results = toolFactories.map { factory ->
            kotlin.concurrent.thread {
                try {
                    factory.adapter.scan(domain, scanId)
                } catch (e: Exception) {
//                    log("Tool ${factory.name} failed: ${e.message}", scanId)
                    emptyList<ScanResult>()
                }
            }
        }.map { it.join(); it }
        println(results)
    }
}