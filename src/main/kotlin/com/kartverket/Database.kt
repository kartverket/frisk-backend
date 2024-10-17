package com.kartverket

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.util.logging.KtorSimpleLogger
import org.flywaydb.core.Flyway
import java.io.File
import java.net.URI
import java.sql.Connection

object Database {
    private lateinit var dataSource: HikariDataSource
    private val logger = KtorSimpleLogger("Database")

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
                        }
                    }
                    else -> {
                        val caCertPath = "/app/db-ssl-ca/server.crt"
                        jdbcUrl = "jdbc:postgresql://${System.getenv(
                            "DATABASE_HOST",
                        )}:5432/frisk-backend-db?sslmode=verify-ca&sslrootcert=$caCertPath"
                        username = System.getenv("DATABASE_USER") ?: ""
                        password = System.getenv("DATABASE_PASSWORD") ?: ""
                    }
                }

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
        logger.info("Database username: ${hikariConfig.username}")
        logger.info("Database password: ${hikariConfig.password}")
        logger.info("Database jdbcUrl: ${hikariConfig.jdbcUrl}")
        dataSource = HikariDataSource(hikariConfig)
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
                    flywayConfig.dataSource("jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}?sslmode=disable", user, pass)
                }
            } else {
                val host = System.getenv("DATABASE_HOST")
                val username = System.getenv("DATABASE_USER")
                val password = System.getenv("DATABASE_PASSWORD")
                // Create database URL from these variables
                val jdbcUrl = "jdbc:postgresql://$host:5432/frisk-backend-db/?sslmode=verify-ca"
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
