package adapter

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class ScanAdapterTest {

    private lateinit var server: MockWebServer

    private val adapter = object : ScanAdapter {
        override suspend fun scan(domain: String, scanId: String) {}
    }

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sendRequest sends POST request to webhook endpoint`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val requestBody = """{"test":"data"}""".toRequestBody("application/json".toMediaTypeOrNull())
        val url = server.url("/webhook").toString()

        adapter.sendRequest(requestBody, url)

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("POST", request?.method)
        assertEquals("/webhook", request?.path)
        assertEquals("""{"test":"data"}""", request?.body?.readUtf8())
    }
}
