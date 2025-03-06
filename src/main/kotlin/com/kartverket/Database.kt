package com.kartverket

import com.kartverket.configuration.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.Serializable
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.ResultSet

object Database {
    lateinit var dataSource: HikariDataSource
    private val logger = KtorSimpleLogger("Database")

    fun getDump(): List<DumpRow> {
        val query =
            """
    select * from functions f
    inner join function_metadata fm on fm.function_id = f.id
    inner join function_metadata_keys fmk on fmk.id = fm.key_id
""".trimIndent()
        getConnection().use { connection ->
            connection.prepareStatement(query).use { preparedStatement ->
                val resultSet = preparedStatement.executeQuery()
                return buildList {
                    while (resultSet.next()) {
                        add(resultSet.toDumpRow())
                    }
                }
            }
        }
    }

    fun initDatabase(databaseConfig: DatabaseConfig) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = databaseConfig.jdbcUrl
            username = databaseConfig.username
            password = databaseConfig.password
            driverClassName = "org.postgresql.Driver"
        }
        logger.info("Database jdbcUrl: ${hikariConfig.jdbcUrl}")
        try {
            dataSource = HikariDataSource(hikariConfig)
        } catch (e: Exception) {
            logger.error("Failed to create the HikariDataSource.", e)
            throw e
        }
    }

    fun getConnection(): Connection = dataSource.connection

    fun closePool() {
        dataSource.close()
    }

    fun migrate() {
        val flywayConfig = Flyway.configure()
        flywayConfig.dataSource(dataSource)

        flywayConfig.locations("classpath:db/migration")
        val flyway = flywayConfig.load()

        val result = flyway.migrate()
        if (result.success) {
            logger.info("Database migrations applied successfully.")
        } else {
            logger.error("Failed to apply database migrations.")
            // Handle the failure appropriately
        }
    }
}

private fun ResultSet.toDumpRow(): DumpRow {
    return DumpRow(
        id = getInt("id"),
        parentId = getInt("parent_id"),
        name = getString("name"),
        description = getString("description"),
        path = getString("path"),
        key = getString("key"),
        value = getString("value")
    )
}

fun List<DumpRow>.toCsv(): String {
    return buildString {
        appendLine("id,name,description,path,key,value")
        for (row in this@toCsv) {
            appendLine("\"${row.id}\",\"${row.name}\",\"${row.description}\",\"${row.path}\",\"${row.key}\",\"${row.value}\"")
        }
    }
}

@Serializable
data class DumpRow(
    val id: Int,
    val name: String,
    val description: String?,
    val parentId: Int?,
    val path: String,
    val key: String,
    val value: String,
)