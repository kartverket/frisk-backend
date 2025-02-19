package com.kartverket.configuration

import io.ktor.server.config.*

object AppConfig {
    lateinit var functionHistoryCleanup: FunctionHistoryCleanupConfig
    lateinit var superUser: SuperUser

    fun load(config: ApplicationConfig) {
        functionHistoryCleanup = FunctionHistoryCleanupConfig(
            cleanupIntervalWeeks = config.propertyOrNull("functionHistoryCleanup.cleanupIntervalWeeks")?.getString()?.toInt() ?: throw IllegalStateException("Unable to initialize app config \"functionHistoryCleanup.cleanupIntervalWeeks\""),
            deleteOlderThanDays = config.propertyOrNull("functionHistoryCleanup.deleteOlderThanDays")?.getString()?.toInt() ?: throw IllegalStateException("Unable to initialize app config \"functionHistoryCleanup.deleteOlderThanDays\""),
        )
        superUser = SuperUser(
            teamId = config.propertyOrNull("superUser.teamId")?.getString(),
        )
    }
}

data class FunctionHistoryCleanupConfig(
    val cleanupIntervalWeeks: Int,
    val deleteOlderThanDays: Int
)

data class SuperUser (
    val teamId: String?,
)