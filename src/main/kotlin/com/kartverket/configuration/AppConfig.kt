package com.kartverket.configuration

import io.ktor.server.config.*

class AppConfig(
    val functionHistoryCleanup: FunctionHistoryCleanupConfig,
    val allowedCORSHosts: List<String>
) {

    companion object {
        fun load(config: ApplicationConfig): AppConfig {
            val allowedCORSHosts = System.getenv("ALLOWED_CORS_HOSTS").split(",")
            return AppConfig(
                functionHistoryCleanup = FunctionHistoryCleanupConfig(
                    cleanupIntervalWeeks = config.propertyOrNull("functionHistoryCleanup.cleanupIntervalWeeks")?.getString()?.toInt() ?: throw IllegalStateException("Unable to initialize app config \"functionHistoryCleanup.cleanupIntervalWeeks\""),
                    deleteOlderThanDays = config.propertyOrNull("functionHistoryCleanup.deleteOlderThanDays")?.getString()?.toInt() ?: throw IllegalStateException("Unable to initialize app config \"functionHistoryCleanup.deleteOlderThanDays\""),
                ),
                allowedCORSHosts = allowedCORSHosts
            )
        }
    }

}

data class FunctionHistoryCleanupConfig(
    val cleanupIntervalWeeks: Int,
    val deleteOlderThanDays: Int
)