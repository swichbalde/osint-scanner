import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import orchestrator.RetrieveOrchestrator
import orchestrator.ScanOrchestrator
import repo.DatabaseFactory

fun main(args: Array<String>) = OsintScannerCli().main(args)

class OsintScannerCli : CliktCommand(
    name = "osint-scanner",
    help = "OSINT Scanner CLI"
) {
    val mode by argument().choice("scan", "retrieve")
    val outputType by option("-o", "--output").choice("excel", "stdout")
    val target by argument(help = "Domain (for scan) or Scan ID (for retrieve)")

    override fun run() {
        DatabaseFactory.init()
        when (mode) {
            "scan" -> {
                ScanOrchestrator().runScan(target)
            }

            "retrieve" -> {
                RetrieveOrchestrator().retrieveScan(outputType, target)
            }
        }
    }
}