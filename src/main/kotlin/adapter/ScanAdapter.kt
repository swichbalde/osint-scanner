package adapter

import repo.ScanResult

interface ScanAdapter {
    fun scan(domain: String, scanId: String): ScanResult
}

data class ToolFactory(val name: String, val adapter: ScanAdapter)

class HarvesterAdapter : ScanAdapter {
    override fun scan(domain: String, scanId: String): ScanResult {
        TODO("Not yet implemented")
    }

}
class AmassAdapter : ScanAdapter {
    override fun scan(domain: String, scanId: String): ScanResult {
        TODO("Not yet implemented")
    }

}