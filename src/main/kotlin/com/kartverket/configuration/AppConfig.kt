package com.kartverket.configuration

import io.ktor.server.config.*

class AppConfig(
    val functionHistoryCleanup: FunctionHistoryCleanupConfig
) {

    companion object {
        fun load(config: ApplicationConfig): AppConfig {
            return AppConfig(
                functionHistoryCleanup = FunctionHistoryCleanupConfig(
                    cleanupIntervalWeeks = config.propertyOrNull("functionHistoryCleanup.cleanupIntervalWeeks")?.getString()?.toInt() ?: throw IllegalStateException("Unable to initialize app config \"functionHistoryCleanup.cleanupIntervalWeeks\""),
                    deleteOlderThanDays = config.propertyOrNull("functionHistoryCleanup.deleteOlderThanDays")?.getString()?.toInt() ?: throw IllegalStateException("Unable to initialize app config \"functionHistoryCleanup.deleteOlderThanDays\""),
                )
            )
        }
    }

}

data class FunctionHistoryCleanupConfig(
    val cleanupIntervalWeeks: Int,
    val deleteOlderThanDays: Int
)