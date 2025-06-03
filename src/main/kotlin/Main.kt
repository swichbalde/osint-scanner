import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import repo.DatabaseFactory
import repo.ScanResult
import repo.Scans
import java.util.*

fun main(args: Array<String>) = OsintScannerCli().main(args)

class OsintScannerCli : CliktCommand(
    name = "osint-scanner",
    help = "OSINT Scanner CLI"//TODO beatify help
) {
    val mode by argument().choice("scan", "retrieve")
    val outputType by option("-o", "--output").choice("excel", "stdout").required()
    val target by argument(help = "Domain (for scan) or Scan ID (for retrieve)")

    override fun run() {
        DatabaseFactory.init()
        when (mode) {
            "scan" -> {
                transaction {
                    Scans.insert {
                        it[id] = UUID.randomUUID().toString()
                        it[domain] = target
                        it[status] = "COMPLETED"
                        it[result] = ScanResult("countryInfo", "whoisJson")
                    }
                }
            }

            "retrieve" -> {
                val scan = transaction {
                    Scans.selectAll().where { Scans.id eq target }.singleOrNull()
                } ?: return
                println(scan)
                println(scan[Scans.result].whoisJson)
                println(scan[Scans.result].countryInfo)
            }
        }
    }
}