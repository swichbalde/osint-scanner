package orchestrator

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import repo.ScanRepo
import repo.ScanResultDto
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class RetrieveOrchestratorTest {

    @BeforeEach
    fun setup() {
        mockkConstructor(ScanRepo::class)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `retrieveScan does nothing if scan not found`() {
        every { anyConstructed<ScanRepo>().retrieveScanById("not_found") } returns null

        RetrieveOrchestrator().retrieveScan(scanId = "not_found")
    }

    @Test
    fun `retrieveScan prints prettyPrint to stdout when outputType is stdout`() {
        val mockScan = mockk<ScanResultDto>()
        every { anyConstructed<ScanRepo>().retrieveScanById("123") } returns mockScan
        every { mockScan.prettyPrint() } returns "pretty result"

        val stdOut = captureStdout {
            RetrieveOrchestrator().retrieveScan(outputType = "stdout", scanId = "123")
        }

        assertTrue(stdOut.trim().contains("pretty result"))
    }

    @Test
    fun `retrieveScan exports to Excel when outputType is excel`() {
        val mockScan = mockk<ScanResultDto>(relaxed = true)
        every { anyConstructed<ScanRepo>().retrieveScanById("456") } returns mockScan

        RetrieveOrchestrator().retrieveScan(outputType = "excel", scanId = "456")

        verify {
            mockScan.exportToExcel("/Users/swichblade-/Developer/osint-scanner/docker/scan_results/scan_456.xlsx")
        }//TODO fix absolute
    }

    private fun captureStdout(block: () -> Unit): String {
        val originalOut = System.out
        val output = ByteArrayOutputStream()
        System.setOut(PrintStream(output))
        try {
            block()
        } finally {
            System.setOut(originalOut)
        }
        return output.toString()
    }
}
