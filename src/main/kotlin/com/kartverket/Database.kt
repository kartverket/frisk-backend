package com.kartverket

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import org.flywaydb.core.Flyway
import java.net.URI
import io.ktor.util.logging.KtorSimpleLogger

object Database {
    private lateinit var dataSource: HikariDataSource
    private val logger = KtorSimpleLogger("Database")

    fun initDatabase() {
        val env = System.getenv("environment")
        val hikariConfig = HikariConfig()
        if (env == "production") {
            logger.info("Using production database configuration")
            val databaseUrl = System.getenv("DATABASE_URL")
            val dbUri = URI(databaseUrl)
            val username = dbUri.userInfo.split(":")[0]
            val password = dbUri.userInfo.split(":")[1]
            val jdbcUrl = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}?sslmode=disable"

            hikariConfig.apply {
                this.jdbcUrl = jdbcUrl
                this.username = username
                this.password = password
                driverClassName = "org.postgresql.Driver"
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
        dataSource = HikariDataSource(hikariConfig)
    }

    fun getConnection(): Connection {
        return dataSource.connection
    }

    // Optionally, you might want to close the pool when your application shuts down
    fun closePool() {
        dataSource.close()
    }

    fun migrate() {
        val env = System.getenv("environment")
        val databaseUrl = System.getenv("DATABASE_URL")
        val flywayConfig = Flyway.configure()
        if (env == "production") {
            // Production environment (Fly.io)
            val dbUri = URI(databaseUrl)
            val username = dbUri.userInfo.split(":")[0]
            val password = dbUri.userInfo.split(":")[1]
            val jdbcUrl = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}?sslmode=disable"

            flywayConfig.dataSource(jdbcUrl, username, password)
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