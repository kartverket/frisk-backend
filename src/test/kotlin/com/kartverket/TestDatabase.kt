package com.kartverket

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

class TestDatabase {
    private var isDatabaseInitialized = false
    private val postgresContainer = PostgreSQLContainer("postgres:15-alpine").apply {
        start()
    }

    fun setupTestDatabase() {
        if (isDatabaseInitialized) return
        isDatabaseInitialized = true

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = postgresContainer.jdbcUrl
            username = postgresContainer.username
            password = postgresContainer.password
            driverClassName = "org.postgresql.Driver"
        }

        Database.dataSource = HikariDataSource(hikariConfig)
        val flyway = Flyway.configure()
            .validateMigrationNaming(true)
            .createSchemas(true)
            .dataSource(Database.dataSource)
            .locations("classpath:db/migration")
            .load()

        flyway.migrate()
    }

    fun stopTestDatabase() {
        Database.closePool()
        postgresContainer.stop()
    }
}