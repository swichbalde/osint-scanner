package repo

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.Paths

class ScanDtoTest {

    @Test
    fun `getScanId extracts id from filename`() {
        val path = Paths.get("/some/path/scan_result_12345.json")
        val scanDto = ScanDto(
            results = emptyList(),
            fullPath = path,
            targetDomain = "example.com"
        )

        assertEquals("12345", scanDto.getScanId())
    }

    @Test
    fun `getScanId handles complex filenames`() {
        val path = Paths.get("/path/with_underscore/scan_result_with_multiple_underscores_67890.json")
        val scanDto = ScanDto(
            results = emptyList(),
            fullPath = path,
            targetDomain = "example.com"
        )

        assertEquals("67890", scanDto.getScanId())
    }
}

class ScanResultDtoTest {

    private lateinit var scanResultDto: ScanResultDto

    @BeforeEach
    fun setup() {
        val scanResults = listOf(
            ScanResult("URL", listOf("https://example.com", "https://test.com")),
            ScanResult("EMAIL", listOf("user@example.com"))
        )

        scanResultDto = ScanResultDto(
            scanId = "12345",
            scanResult = scanResults,
            domain = "example.com",
            fileName = "scan_result.json",
            fileContentSize = 1024
        )
    }

    @Test
    fun `prettyPrint formats output correctly`() {
        val expected = """
            === Scan Result ===
            Scan ID: 12345
            Domain: example.com
            Results:
              - Type: URL
                  https://example.com
                  https://test.com
              - Type: EMAIL
                  user@example.com
            ===================
        """.trimIndent()

        assertEquals(expected, scanResultDto.prettyPrint().trim())
    }

    @Test
    fun `exportToExcel creates valid excel file with correct data`(@TempDir tempDir: Path) {
        val filePath = tempDir.resolve("test_export.xlsx").toString()

        scanResultDto.exportToExcel(filePath)

        val file = File(filePath)
        assertTrue(file.exists())

        FileInputStream(file).use { fis ->
            val workbook = XSSFWorkbook(fis)
            val sheet = workbook.getSheetAt(0)

            assertEquals("ScanResults", sheet.sheetName)

            val headerRow = sheet.getRow(0)
            assertEquals("Scan ID", headerRow.getCell(0).stringCellValue)
            assertEquals("Domain", headerRow.getCell(1).stringCellValue)
            assertEquals("Type", headerRow.getCell(2).stringCellValue)
            assertEquals("Artifact", headerRow.getCell(3).stringCellValue)

            val row1 = sheet.getRow(1)
            assertEquals("12345", row1.getCell(0).stringCellValue)
            assertEquals("example.com", row1.getCell(1).stringCellValue)
            assertEquals("URL", row1.getCell(2).stringCellValue)
            assertEquals("https://example.com", row1.getCell(3).stringCellValue)

            val row2 = sheet.getRow(2)
            assertEquals("12345", row2.getCell(0).stringCellValue)
            assertEquals("example.com", row2.getCell(1).stringCellValue)
            assertEquals("URL", row2.getCell(2).stringCellValue)
            assertEquals("https://test.com", row2.getCell(3).stringCellValue)

            val row3 = sheet.getRow(3)
            assertEquals("12345", row3.getCell(0).stringCellValue)
            assertEquals("example.com", row3.getCell(1).stringCellValue)
            assertEquals("EMAIL", row3.getCell(2).stringCellValue)
            assertEquals("user@example.com", row3.getCell(3).stringCellValue)

            workbook.close()
        }
    }
}
