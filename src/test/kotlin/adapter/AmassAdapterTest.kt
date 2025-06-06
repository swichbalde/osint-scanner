package adapter

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.RequestBody
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AmassAdapterTest {

    private lateinit var adapter: AmassAdapter

    @BeforeEach
    fun setup() {
        adapter = spyk<AmassAdapter>(recordPrivateCalls = true)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `scan sends correct request`() = runBlocking {
        val domain = "example.com"
        val scanId = "123"
        val expectedEndpoint = "http://amass:9000/hooks/amass-scan"
        val expectedJson = """
            {
                "domain": "$domain",
                "output_file": "amass_${scanId}.txt"
            }
        """.trimIndent()

        coEvery { adapter.sendRequest(any(), any()) } just Runs

        adapter.scan(domain, scanId)

        coVerify(exactly = 1) {
            adapter.sendRequest(
                match { requestBodyToString(it) == expectedJson },
                expectedEndpoint
            )
        }
    }

    private fun requestBodyToString(requestBody: RequestBody): String {
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        return buffer.readUtf8()
    }
}


class WaitForFileCompleteTest {

    private lateinit var path: Path
    private lateinit var file: File

    @BeforeEach
    fun setup() {
        path = mockk()
        file = mockk()
        every { path.toFile() } returns file
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `returns true when file size stabilizes`() = runTest {
        val sizes = listOf(100L, 200L, 200L, 200L)
        var idx = 0
        every { file.length() } answers { sizes.getOrElse(idx++) { 200L } }

        val result = waitForFileComplete(
            path,
            stableDurationSeconds = 1,
            maxTotalSeconds = 20
        )

        assertTrue(result)
    }

    @Test
    fun `returns false when file size never stabilizes`() = runTest {
        val sizes = listOf(100L, 200L, 300L, 400L, 500L)
        var idx = 0
        every { file.length() } answers { sizes[idx++ % sizes.size] }

        val result = waitForFileComplete(
            path,
            stableDurationSeconds = 2,
            maxTotalSeconds = 10
        )

        assertFalse(result)
    }

    @Test
    fun `returns true when file size stabilizes after some exceptions`() = runTest {
        val sizes = listOf(
            { 0L },
            { 100L },
            { 100L },
            { 100L }
        )
        var idx = 0
        every { file.length() } answers { sizes.getOrElse(idx) { { 100L } }().also { idx++ } }

        val result = waitForFileComplete(
            path,
            stableDurationSeconds = 1,
            maxTotalSeconds = 20
        )

        assertTrue(result)
    }

    @Test
    fun `returns false if file size is always zero`() = runTest {
        every { file.length() } returns 0L

        val result = waitForFileComplete(
            path,
            stableDurationSeconds = 2,
            maxTotalSeconds = 10
        )

        assertFalse(result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `respects backoff delays and maxTotalSeconds`() = runTest {
        every { file.length() } returnsMany listOf(1L, 2L, 3L, 4L, 5L)

        val start = currentTime
        val result = waitForFileComplete(
            path,
            stableDurationSeconds = 1,
            maxTotalSeconds = 5
        )
        val elapsed = currentTime - start

        assertFalse(result)
        assertTrue(elapsed <= 12000)
    }
}


class ParseAmassFileGroupedTest {

    private lateinit var tempFile: Path

    @BeforeEach
    fun setup() {
        tempFile = Files.createTempFile("amass", ".txt")
    }

    @AfterEach
    fun cleanup() {
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `parses and groups valid lines`() {
        val line1 = "foo (A) --> bar --> baz (B)"
        val line2 = "qux (A) --> quux --> corge (B)"
        val line3 = "abc (C) --> xyz --> def (D)"
        tempFile.writeText("$line1\n$line2\n$line3")
        val results = parseAmassFileGrouped(tempFile)
        assertEquals(2, results.size)
        val types = results.map { it.type }.toSet()
        assertTrue(types.contains("A -> B"))
        assertTrue(types.contains("C -> D"))
    }

    @Test
    fun `returns empty for file with no valid lines`() {
        tempFile.writeText("invalid line\nanother bad line")
        val results = parseAmassFileGrouped(tempFile)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `handles empty file`() {
        tempFile.writeText("")
        val results = parseAmassFileGrouped(tempFile)
        assertTrue(results.isEmpty())
    }
}
