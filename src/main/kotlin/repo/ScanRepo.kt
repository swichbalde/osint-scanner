package repo

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.FileOutputStream
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

data class ScanDto(
    val results: List<ScanResult>?,
    val fullPath: Path,
    val targetDomain: String,
) {
    fun getScanId() =
        fullPath.fileName.toString().split("_").last().split(".").first()
}

data class ScanResultDto(
    val scanId: String,
    val scanResult: List<ScanResult>,
    val domain: String,
    val fileName: String,
    val fileContentSize: Int,
) {
    fun prettyPrint(): String = buildString {
        appendLine("=== Scan Result ===")
        appendLine("Scan ID: $scanId")
        appendLine("Domain: $domain")
        appendLine("File: $fileName")
        appendLine("File size: $fileContentSize")
        appendLine("Results:")
        scanResult.forEach { result ->
            appendLine("  - Type: ${result.type}")
            result.value.forEach { v ->
                appendLine("      $v")
            }
        }
        appendLine("===================")
    }

    fun exportToExcel(filePath: String) {
        val workbook: Workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("ScanResults")

        // Header
        val header = listOf("Scan ID", "Domain", "File Name", "Type", "Artifact")
        val headerRow: Row = sheet.createRow(0)
        header.forEachIndexed { idx, name ->
            headerRow.createCell(idx).setCellValue(name)
        }

        var rowIdx = 1
        for (result in scanResult) {
            for (artifact in result.value) {
                val row = sheet.createRow(rowIdx++)
                row.createCell(0).setCellValue(scanId)
                row.createCell(1).setCellValue(domain)
                row.createCell(2).setCellValue(fileName)
                row.createCell(3).setCellValue(result.type)
                row.createCell(4).setCellValue(artifact)
            }
        }

        // Autosize columns
        header.indices.forEach { sheet.autoSizeColumn(it) }

        FileOutputStream(filePath).use { workbook.write(it) }
        workbook.close()
    }
}

class ScanRepo {
    fun saveScanResult(scanDto: ScanDto) {
        val results = scanDto.results
        val fullPath = scanDto.fullPath

        logger.info { "Starting to save scan result for file: $fullPath, domain: ${scanDto.targetDomain}" }

        if (results.isNullOrEmpty()) {
            logger.warn { "No results to save for scan: ${scanDto.getScanId()}, file: $fullPath" }
            return
        }

        val filename = fullPath.fileName.toString()
        logger.info { "Saving raw file data for: $filename" }
        val fileId = try {
            transaction {
                RawFileTable.insert {
                    it[fileName] = filename
                    it[data] = fullPath.toFile().readBytes()
                } get RawFileTable.id
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save raw file data for $filename: ${e.message}" }
            throw e
        }
        logger.info { "Saved raw file with id: $fileId for $filename" }

        results.forEach { harvesterRecord ->
            try {
                transaction {
                    ScanTable.insert {
                        it[scanId] = scanDto.getScanId()
                        it[domain] = scanDto.targetDomain
                        it[result] = ScanResult(harvesterRecord.type, harvesterRecord.value)
                        it[rawFileId] = fileId
                    }
                }
                logger.info { "Saved scan result for type: ${harvesterRecord.type}, entries: ${harvesterRecord.value.size}, scanId: ${scanDto.getScanId()}" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to save scan result for type: ${harvesterRecord.type}, scanId: ${scanDto.getScanId()}: ${e.message}" }
            }
        }
        logger.info { "Completed saving scan results for file: $filename, scanId: ${scanDto.getScanId()}" }
    }

    fun retrieveScanById(scanId: String): ScanResultDto? {
        logger.info { "Starting to retrieve scan result for file: $scanId" }
        val results = transaction {
            ScanTable.join(RawFileTable, JoinType.LEFT).selectAll()
                .where { ScanTable.scanId eq scanId }
                .map { row ->
                    Pair(
                        ScanResult(
                            type = row[ScanTable.result].type,
                            value = row[ScanTable.result].value
                        ),
                        row
                    )
                }
        }
        if (results.isEmpty()) return null

        val firstRow = results.first().second
        return ScanResultDto(
            scanId = scanId,
            scanResult = results.map { it.first },
            domain = firstRow[ScanTable.domain],
            fileName = firstRow[RawFileTable.fileName],
            fileContentSize = firstRow[RawFileTable.data].size
        )
    }

}