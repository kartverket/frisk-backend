package com.kartverket

import com.kartverket.configuration.AppConfig
import com.kartverket.configuration.FunctionHistoryCleanupConfig
import com.kartverket.plugins.*
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

fun CoroutineScope.launchCleanupJob(functionHistoryCleanupConfig: FunctionHistoryCleanupConfig): Job {
    val cleanupIntervalWeeks = functionHistoryCleanupConfig.cleanupIntervalWeeks
    val cleanupInterval: Duration = (cleanupIntervalWeeks * 7).days

    return launch(Dispatchers.IO) {
        while (isActive) {
            try {
                logger.info("Running scheduled cleanup every $cleanupIntervalWeeks weeks.")
                cleanupFunctionsHistory(functionHistoryCleanupConfig.deleteOlderThanDays)
            } catch (e: Exception) {
                logger.error("Error during function history cleanup: ${e.message}")
            }
            delay(cleanupInterval.inWholeMilliseconds)
        }
    }
}

fun cleanupFunctionsHistory(deleteOlderThanDays: Int) {
    logger.info("Running scheduled cleanup for functions_history table. Deleting entries older than $deleteOlderThanDays days.")

    Database.getConnection().use { conn ->
        conn.prepareStatement("DELETE FROM functions_history WHERE valid_from < NOW() - make_interval(days := ?)")
            .use { stmt ->
                stmt.setInt(1, deleteOlderThanDays)
                val deletedRows = stmt.executeUpdate()
                logger.info("Cleanup completed. Deleted $deletedRows rows.")
            }
    }
}

fun Application.module() {
    val config = AppConfig.load(environment.config)
    Database.initDatabase(config.databaseConfig)
    Database.migrate()
    configureAPILayer(config)
    launchCleanupJob(config.functionHistoryCleanup)

    environment.monitor.subscribe(ApplicationStopped) {
        Database.closePool()
    }
}

fun Application.configureAPILayer(config: AppConfig) {
    configureSerialization()
    configureCors(config)
    configureAuth()
    configureRouting()
}