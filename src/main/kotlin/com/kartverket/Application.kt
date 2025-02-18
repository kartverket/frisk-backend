package com.kartverket

import com.kartverket.configuration.AppConfig
import com.kartverket.plugins.*
import com.kartverket.util.NewSchemaMetadataMapper
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

fun main() {
    embeddedServer(
        Netty,
        environment = applicationEngineEnvironment {
            config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
            module(Application::module)
            connector {
                port = 8080
                host = "0.0.0.0"
            }
        }
    ).start(wait = true)
}

private fun loadAppConfig(config: ApplicationConfig) {
    AppConfig.load(config)
}

fun CoroutineScope.launchCleanupJob(): Job {
    val cleanupIntervalWeeks = AppConfig.functionHistoryCleanup.cleanupIntervalWeeks
    val cleanupInterval: Duration = (cleanupIntervalWeeks * 7).days

    return launch(Dispatchers.IO) {
        while (isActive) {
            try {
                logger.info("Running scheduled cleanup every $cleanupIntervalWeeks weeks.")
                cleanupFunctionsHistory()
            } catch (e: Exception) {
                logger.error("Error during function history cleanup: ${e.message}")
            }
            delay(cleanupInterval.inWholeMilliseconds)
        }
    }
}

fun cleanupFunctionsHistory() {
    val deleteOlderThanDays = AppConfig.functionHistoryCleanup.deleteOlderThanDays
    logger.info("Running scheduled cleanup for functions_history table. Deleting entries older than $deleteOlderThanDays days.")

    Database.getConnection().use { conn ->
        conn.prepareStatement("DELETE FROM functions_history WHERE valid_from < NOW() - INTERVAL '$deleteOlderThanDays days'")
            .use { stmt ->
                val deletedRows = stmt.executeUpdate()
                logger.info("Cleanup completed. Deleted $deletedRows rows.")
            }
    }
}


fun Application.module() {
    loadAppConfig(environment.config)

    Database.initDatabase()
    Database.migrate()

    configureSerialization()
    configureCors()
    configureAuth()
    configureRouting()
    launch {
        NewSchemaMetadataMapper().addNewSchemaMetadata()
    }

    launchCleanupJob()

    environment.monitor.subscribe(ApplicationStopped) {
        Database.closePool()
    }
}
