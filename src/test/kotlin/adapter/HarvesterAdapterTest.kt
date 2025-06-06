package adapter

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import repo.ScanResult
import kotlin.test.assertEquals

class HarvesterAdapterTest {

    private lateinit var adapter: HarvesterAdapter

    @BeforeEach
    fun setUp() {
        adapter = spyk(HarvesterAdapter(), recordPrivateCalls = true)
    }

    @Test
    fun `scan should call sendRequest with correct body and endpoint`() = runTest {
        val domain = "example.com"
        val scanId = "123"
        val expectedBodyString = """
            {
                "domain": "$domain",
                "source": "bing",
                "output_file": "harvester_$scanId"
            }
        """.trimIndent()
        val expectedBody = expectedBodyString.toRequestBody("application/json".toMediaType())
        val expectedEndpoint = "http://localhost:9001/hooks/harvester-scan"

        coEvery { adapter.sendRequest(any(), any()) } just Runs

        adapter.scan(domain, scanId)

        verify {
            adapter["sendRequest"](match<RequestBody> {
                it.contentType() == expectedBody.contentType() &&
                        it.contentLength() == expectedBody.contentLength()
            }, expectedEndpoint)
        }
    }
}

class HarvesterAdapterTestParse {

    @Test
    fun `returns empty list on invalid JSON`() {
        val invalidJson = "{not a valid json"
        val result = parseHarvesterFile(invalidJson)
        assertEquals(emptyList(), result)
    }

    @Test
    fun `returns empty list when JSON is not an object`() {
        val jsonArray = "[1, 2, 3]"
        val result = parseHarvesterFile(jsonArray)
        assertEquals(emptyList(), result)
    }

    @Test
    fun `returns empty list when JSON object has no arrays`() {
        val jsonObj = """{"a": "string", "b": 123}"""
        val result = parseHarvesterFile(jsonObj)
        assertEquals(emptyList(), result)
    }

    @Test
    fun `returns list for one array field`() {
        val jsonObj = """{"emails": ["a@example.com", "b@example.com"]}"""
        val result = parseHarvesterFile(jsonObj)
        assertEquals(
            listOf(ScanResult("emails", listOf("a@example.com", "b@example.com"))),
            result
        )
    }

    @Test
    fun `returns list for multiple array fields`() {
        val jsonObj = """
            {
                "emails": ["a@example.com"],
                "hosts": ["host1", "host2"],
                "irrelevant": 123
            }
        """.trimIndent()
        val result = parseHarvesterFile(jsonObj)
        assertEquals(
            listOf(
                ScanResult("emails", listOf("a@example.com")),
                ScanResult("hosts", listOf("host1", "host2"))
            ),
            result
        )
    }

    @Test
    fun `handles empty array field`() {
        val jsonObj = """{"hosts": []}"""
        val result = parseHarvesterFile(jsonObj)
        assertEquals(
            listOf(ScanResult("hosts", emptyList())),
            result
        )
    }
}