package com.kartverket.configuration

import io.ktor.server.config.*
import org.slf4j.LoggerFactory
import java.net.URI

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
            return if (env == "production") {
                val platform = System.getenv("platform")
                if (platform == "flyio") {
                    logger.info("Using Fly.io database configuration")
                    System.getenv("DATABASE_URL").let { databaseUrl ->
                        val dbUri = URI(databaseUrl)
                        val (user, pass) = dbUri.userInfo.split(":", limit = 2)
                        DatabaseConfig(
                            jdbcUrl = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}?sslmode=disable",
                            username = user,
                            password = pass,
                        )
                    }
                } else {
                    logger.info("Using gcp database configuration")
                    val serverCertPath = "/app/db-ssl-ca/server-ca.pem"
                    val clientCertPath = "/app/db-ssl-ca/client-cert.pem"
                    val clientKeyPath = "/app/db-ssl-ca/client-key.pk8"

                    DatabaseConfig(
                        jdbcUrl = "jdbc:postgresql://${
                            System.getenv(
                                "DATABASE_HOST",
                            )
                        }:5432/frisk-backend-db?sslmode=require&sslrootcert=$serverCertPath$&sslcert=$clientCertPath&sslkey=$clientKeyPath",
                        username = "admin",
                        password = System.getenv("DATABASE_PASSWORD") ?: ""
                    )
                }
            } else {
                logger.info("Using local development database configuration")
                val jdbcUrl = "jdbc:postgresql://localhost:5432/frisk-backend-db"
                val username = "postgres"
                val password = ""

                DatabaseConfig(
                    jdbcUrl = jdbcUrl,
                    username = username,
                    password = password
                )
            }
        }
    }
}