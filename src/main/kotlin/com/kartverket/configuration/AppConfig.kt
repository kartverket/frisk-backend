package com.kartverket.configuration

import io.ktor.server.config.*

data class AppConfig(
    val functionHistoryCleanup: FunctionHistoryCleanupConfig,
    val allowedCORSHosts: List<String>,
    val databaseConfig: DatabaseConfig
) {
    companion object {
        fun load(config: ApplicationConfig): AppConfig {
            val allowedCORSHosts = System.getenv("ALLOWED_CORS_HOSTS").split(",")

            return AppConfig(
                functionHistoryCleanup = FunctionHistoryCleanupConfig(
                    cleanupIntervalWeeks = config.propertyOrNull("functionHistoryCleanup.cleanupIntervalWeeks")
                        ?.getString()?.toInt()
                        ?: throw IllegalStateException("Unable to initialize app config \"functionHistoryCleanup.cleanupIntervalWeeks\""),
                    deleteOlderThanDays = config.propertyOrNull("functionHistoryCleanup.deleteOlderThanDays")
                        ?.getString()?.toInt()
                        ?: throw IllegalStateException("Unable to initialize app config \"functionHistoryCleanup.deleteOlderThanDays\""),
                ),
                allowedCORSHosts = allowedCORSHosts,
                databaseConfig = DatabaseConfig.load()
            )
        }
    }
}

data class FunctionHistoryCleanupConfig(
    val cleanupIntervalWeeks: Int,
    val deleteOlderThanDays: Int
)

class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
) {
    companion object {
        fun load(): DatabaseConfig = DatabaseConfig(
            jdbcUrl = getConfigFromEnvOrThrow("JDBC_URL"),
            username = getConfigFromEnvOrThrow("DATABASE_USERNAME"),
            password = getConfigFromEnvOrThrow("DATABASE_PASSWORD")
        )
    }
}

fun getConfigFromEnvOrThrow(variableName: String): String =
    System.getenv(variableName)
        ?: throw IllegalStateException("Unable to initialize app config, \"$variableName\" is null")
