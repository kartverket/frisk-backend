package com.kartverket

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.Serializable
import org.flywaydb.core.Flyway
import java.net.URI
import java.sql.Connection
import java.sql.ResultSet

object Database {
    lateinit var dataSource: HikariDataSource
    private val logger = KtorSimpleLogger("Database")

    fun getDump(): List<DumpRow> {
        val query = "select * from functions f inner join function_metadata fm on fm.function_id = f.id inner join function_metadata_keys fmk on fmk.id = fm.key_id"
        val dump = mutableListOf<DumpRow>()
        getConnection().use { connection ->
            connection.prepareStatement(query).use { preparedStatement ->
                val resultSet = preparedStatement.executeQuery()
                while (resultSet.next()) {
                    dump.add(resultSet.toDumpRow())
                }
            }
        }
        return dump
    }

    fun initDatabase() {
        val env = System.getenv("environment")
        val hikariConfig = HikariConfig()
        if (env == "production") {
            hikariConfig.apply {
                val platform = System.getenv("platform")
                when (platform) {
                    "flyio" -> {
                        logger.info("Using Fly.io database configuration")
                        System.getenv("DATABASE_URL")?.let { databaseUrl ->
                            val dbUri = URI(databaseUrl)
                            val (user, pass) = dbUri.userInfo.split(":", limit = 2)
                            jdbcUrl = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}?sslmode=disable"
                            username = user
                            password = pass
                            driverClassName = "org.postgresql.Driver"
                        }
                    }

                    else -> {
                        logger.info("Using gcp database configuration")
                        val serverCertPath = "/app/db-ssl-ca/server-ca.pem"
                        val clientCertPath = "/app/db-ssl-ca/client-cert.pem"
                        val clientKeyPath = "/app/db-ssl-ca/client-key.pk8"

                        jdbcUrl = "jdbc:postgresql://${
                            System.getenv(
                                "DATABASE_HOST",
                            )
                        }:5432/frisk-backend-db?sslmode=require&sslrootcert=$serverCertPath$&sslcert=$clientCertPath&sslkey=$clientKeyPath"
                        username = "admin"
                        password = System.getenv("DATABASE_PASSWORD") ?: ""
                        driverClassName = "org.postgresql.Driver"
                    }
                }
            }
        } else {
            logger.info("Using local development database configuration")
            val jdbcUrl = "jdbc:postgresql://localhost:5432/frisk-backend-db"
            val username = "postgres"
            val password = ""

            hikariConfig.apply {
                this.jdbcUrl = jdbcUrl
                this.username = username
                this.password = password
                driverClassName = "org.postgresql.Driver"
            }
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
        val env = System.getenv("environment")
        val flywayConfig = Flyway.configure()
        if (env == "production") {
            if (System.getenv("platform") == "flyio") {
                logger.info("Using Fly.io database configuration")
                System.getenv("DATABASE_URL")?.let { databaseUrl ->
                    val dbUri = URI(databaseUrl)
                    val (user, pass) = dbUri.userInfo.split(":", limit = 2)
                    flywayConfig.dataSource(
                        "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}?sslmode=disable",
                        user,
                        pass,
                    )
                }
            } else {
                logger.info("Using gcp database configuration for migration")
                val serverCertPath = "/app/db-ssl-ca/server-ca.pem"
                val clientCertPath = "/app/db-ssl-ca/client-cert.pem"
                val clientKeyPath = "/app/db-ssl-ca/client-key.pk8"
                val username = "admin"
                val password = System.getenv("DATABASE_PASSWORD")
                val jdbcUrl = "jdbc:postgresql://${
                    System.getenv(
                        "DATABASE_HOST",
                    )
                }:5432/frisk-backend-db?sslmode=verify-ca&sslrootcert=$serverCertPath&sslcert=$clientCertPath&sslkey=$clientKeyPath"
                flywayConfig.dataSource(jdbcUrl, username, password)
            }
        } else {
            // Local development environment
            val jdbcUrl = "jdbc:postgresql://localhost:5432/frisk-backend-db"
            val username = "postgres"
            val password = ""

            flywayConfig.dataSource(jdbcUrl, username, password)
        }

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