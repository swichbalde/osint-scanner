package listener

import adapter.waitForFileComplete
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import repo.ScanRepo
import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.test.assertNull

class ListenerTest {

    private lateinit var mockWatchService: WatchService
    private lateinit var mockPath: Path
    private lateinit var mockScanRepo: ScanRepo
    private lateinit var mockWatchKey: WatchKey
    private lateinit var mockWatchEvent: WatchEvent<*>

    @BeforeEach
    fun setUp() {
        mockWatchService = mockk()
        mockPath = mockk()
        mockScanRepo = mockk()
        mockWatchKey = mockk()
        mockWatchEvent = mockk()

        every { mockPath.register(any(), *anyVararg()) } returns mockWatchKey
        every { mockPath.toString() } returns "/test/path"
        every { mockPath.resolve(any<Path>()) } answers {
            mockk<Path>().also {
                every { it.fileName } returns firstArg<Path>()
                every { it.toString() } returns "/test/path/${firstArg<Path>().toString()}"
            }
        }
    }

    @Test
    fun `handleNewFile should return null when Amass file is not complete`() = runBlocking {
        val fileName = "amass_test.txt"
        val fullPath = mockk<Path>()
        every { fullPath.fileName } returns mockk<Path>().also { every { it.toString() } returns fileName }
        every { fullPath.toString() } returns "/test/path/$fileName"
        coEvery { waitForFileComplete(fullPath) } returns false

        val result = handleNewFile(fullPath)

        assertNull(result)
    }

    @Test
    fun `handleNewFile should return null for unknown file type`() = runBlocking {
        val fileName = "unknown_test.txt"
        val fullPath = mockk<Path>()
        every { fullPath.fileName } returns mockk<Path>().also { every { it.toString() } returns fileName }
        every { fullPath.toString() } returns "/test/path/$fileName"

        val result = handleNewFile(fullPath)

        assertNull(result)
    }
}