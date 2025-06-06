package orchestrator

import io.github.oshai.kotlinlogging.KotlinLogging
import repo.ScanRepo

private val logger = KotlinLogging.logger {}

class RetrieveOrchestrator {
    fun retrieveScan(outputType: String? = "stdout", scanId: String) {
        val retrievedScan = ScanRepo().retrieveScanById(scanId)
        if (retrievedScan == null) {
            logger.warn { "Scan results not found, scanId: $scanId" }
            return
        }
        when (outputType) {
            "stdout" -> {
                println(retrievedScan.prettyPrint())
            }

            "excel" -> {
                val filePath = "/Users/swichblade-/Developer/osint-scanner/docker/scan_results/scan_${scanId}.xlsx"
                retrievedScan.exportToExcel(filePath)
                logger.info { "Exported scan $scanId to Excel file: $filePath" }
            }
        }
    }
}