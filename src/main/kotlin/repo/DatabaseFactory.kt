package repo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ScanResult(
    val type: String,
    val value: List<String>,
)

val format = Json { prettyPrint = true }

object ScanTable : Table("scan") {
    val id = integer("id").autoIncrement()
    val domain = varchar("domain", 255)
    val scanId = varchar("scan_id", 36).index()
    val result = jsonb<ScanResult>("result", Json.Default)
    val rawFileId = integer("raw_file_id").references(RawFileTable.id)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object RawFileTable : Table("raw_file") {
    val id = integer("id").autoIncrement()
    val fileName = varchar("file_name", 255)
    val data = binary("data")

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object DatabaseFactory {
    fun init() {
        val url = System.getenv("DB_URL")
        val user = System.getenv("DB_USER")
        val password = System.getenv("DB_PASSWORD")
        val driver = System.getenv("DB_DRIVER")


        Database.connect(url, driver = driver, user = user, password = password)
        transaction {
            SchemaUtils.create(ScanTable, RawFileTable)
        }
    }
}