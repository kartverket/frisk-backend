package com.kartverket

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import org.flywaydb.core.Flyway
import io.ktor.server.config.ApplicationConfig

object Database {
    private lateinit var dataSource: HikariDataSource

    fun initDatabase(config: ApplicationConfig) {
        val jdbcUrl = config.property("ktor.database.jdbcURL").getString()
        val jdbcUser = config.property("ktor.database.username").getString()
        val jdbcPassword = config.property("ktor.database.password").getString()
        val driver = config.property("ktor.database.driver").getString()

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            username = jdbcUser
            password = jdbcPassword
            driverClassName = driver

            // Additional configuration
            maximumPoolSize = 10
            minimumIdle = 5
            idleTimeout = 600000 // 10 minutes
            connectionTimeout = 30000 // 30 seconds
            maxLifetime = 1800000 // 30 minutes
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

    fun migrate(config: ApplicationConfig) {
        val jdbcUrl = config.property("ktor.database.jdbcURL").getString()
        val jdbcUser = config.property("ktor.database.username").getString()
        val jdbcPassword = config.property("ktor.database.password").getString()

        val flyway = Flyway.configure()
            .dataSource(jdbcUrl, jdbcUser, jdbcPassword)
            .locations("classpath:db/migration")
            .load()

        val result = flyway.migrate()
        if (result.success) {
            println("Database migrations applied successfully.")
        } else {
            println("Failed to apply database migrations.")
            // Here you might want to throw an exception or handle the failure appropriately
        }
    }
}