package com.kartverket

import com.kartverket.configuration.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

class TestDatabase {
    private val postgresContainer = PostgreSQLContainer("postgres:15-alpine").apply {
        start()
    }
    lateinit var dataSource: HikariDataSource

    fun getTestdatabaseConfig(): DatabaseConfig {
        return DatabaseConfig(
            jdbcUrl = postgresContainer.jdbcUrl,
            username = postgresContainer.username,
            password = postgresContainer.password,
        )
    }

    fun stopTestDatabase() {
        postgresContainer.stop()
    }
}