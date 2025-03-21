package com.kartverket

import com.kartverket.configuration.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.util.logging.KtorSimpleLogger
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.PreparedStatement

interface Database {
    fun getConnection(): Connection
}

class JDBCDatabase(
    private val dataSource: HikariDataSource
) : Database {
    override fun getConnection(): Connection = dataSource.connection

    fun closePool() {
        dataSource.close()
    }

    private fun migrate() {
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

    companion object {
        private val logger = KtorSimpleLogger("Database")

        fun create(databaseConfig: DatabaseConfig): JDBCDatabase {
            val hikariConfig = HikariConfig().apply {
                jdbcUrl = databaseConfig.jdbcUrl
                username = databaseConfig.username
                password = databaseConfig.password
                driverClassName = "org.postgresql.Driver"
            }
            logger.info("Database jdbcUrl: ${hikariConfig.jdbcUrl}")
            return try {
                JDBCDatabase(HikariDataSource(hikariConfig)).also {
                    it.migrate()
                }
            } catch (e: Exception) {
                logger.error("Failed to create the HikariDataSource.", e)
                throw e
            }
        }
    }
}

inline fun <T> Database.useStatement(query: String, block: (statement: PreparedStatement) -> T): T {
    return getConnection().use { connection ->
        connection.prepareStatement(query).use(block)
    }
}
