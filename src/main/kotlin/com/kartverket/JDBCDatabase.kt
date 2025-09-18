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
    private val dataSource: HikariDataSource,
) : Database {
    override fun getConnection(): Connection = dataSource.connection

    fun closePool() {
        dataSource.close()
    }

    private fun migrate() {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .validateMigrationNaming(true)
            .load()
        val result = flyway.migrate()
        if (result.success) {
            logger.info("Database migrations applied successfully.")
        } else {
            throw IllegalStateException("Failed to apply database migrations.")
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
                databaseConfig.minimumIdle?.let { minimumIdle = it }
                databaseConfig.maxPoolSize?.let {maximumPoolSize = it }
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

inline fun <T> Database.useStatement(query: String, block: (statement: PreparedStatement) -> T): T = getConnection().use { connection ->
    connection.prepareStatement(query).use(block)
}
