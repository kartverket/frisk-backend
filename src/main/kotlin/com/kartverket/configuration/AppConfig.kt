package com.kartverket.configuration

import io.ktor.server.config.*
import org.slf4j.LoggerFactory

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
        private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)

        fun load(): DatabaseConfig {
            val env = System.getenv("environment")
            val platform = System.getenv("platform")
            return if (env == "production" && platform != "flyio") {
                logger.info("Using gcp database configuration")

                val skipJdbcUrl = "jdbc:postgresql://${
                    System.getenv(
                        "DATABASE_HOST",
                    )
                }:5432/frisk-backend-db?sslmode=require&sslrootcert=/app/db-ssl-ca/server-ca.pem$&sslcert=/app/db-ssl-ca/client-cert.pem&sslkey=/app/db-ssl-ca/client-key.pk8"
                DatabaseConfig(
                    jdbcUrl = skipJdbcUrl,
                    username = "admin",
                    password = System.getenv("DATABASE_PASSWORD") ?: ""
                )
            } else {
                logger.info("Using local development database configuration")

                DatabaseConfig(
                    jdbcUrl = getConfigFromEnvOrThrow("JDBC_URL"),
                    username = getConfigFromEnvOrThrow("DATABASE_USERNAME"),
                    password = getConfigFromEnvOrThrow("DATABASE_PASSWORD")
                )
            }
        }
    }
}

fun getConfigFromEnvOrThrow(variableName: String): String =
    System.getenv(variableName)
        ?: throw IllegalStateException("Unable to initialize app config, \"$variableName\" is null")
