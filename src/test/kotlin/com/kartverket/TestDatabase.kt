package com.kartverket

import com.kartverket.configuration.DatabaseConfig
import org.testcontainers.containers.PostgreSQLContainer

class TestDatabase {
    private val postgresContainer = PostgreSQLContainer("postgres:15-alpine").apply {
        start()
    }

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
