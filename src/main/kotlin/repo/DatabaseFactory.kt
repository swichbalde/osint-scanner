package repo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@Serializable
data class ScanResult(
    val countryInfo: String,
    val whoisJson: String
)
val format = Json { prettyPrint = true }

object Scans : Table("scans") {
    val id = varchar("id", 36)
    val domain = varchar("domain", 255)
    val status = varchar("status", 20)//TODO ?
    val result = jsonb<ScanResult>("result", Json.Default)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object DatabaseFactory {
    fun init() {
        val fis = javaClass.getResourceAsStream("/application.properties") ?: throw Exception("Application properties not found1")
        val props = Properties().apply {
            load(fis)
        }
        val url = props.getProperty("db.url")
        val user = props.getProperty("db.user")
        val password = props.getProperty("db.password")
        val driver = props.getProperty("db.driver")

        Database.connect(url, driver = driver, user = user, password = password)
        transaction {
            SchemaUtils.create(Scans)
        }
    }
}