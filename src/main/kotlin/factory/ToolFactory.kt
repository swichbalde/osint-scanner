package factory

import adapter.AmassAdapter
import adapter.HarvesterAdapter
import adapter.ScanAdapter

data class ToolFactory(val name: String, val adapter: ScanAdapter)

val toolFactories = listOf(
    ToolFactory("harvester", HarvesterAdapter()),
    ToolFactory("amass", AmassAdapter())
)