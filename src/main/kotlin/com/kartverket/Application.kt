package com.kartverket

import com.kartverket.auth.AuthService
import com.kartverket.auth.AuthServiceImpl
import com.kartverket.auth.configureAuth
import com.kartverket.auth.logger
import com.kartverket.configuration.AppConfig
import com.kartverket.configuration.FunctionHistoryCleanupConfig
import com.kartverket.functions.FunctionService
import com.kartverket.functions.FunctionServiceImpl
import com.kartverket.functions.datadump.DataDumpService
import com.kartverket.functions.datadump.DataDumpServiceImpl
import com.kartverket.functions.metadata.FunctionMetadataService
import com.kartverket.functions.metadata.FunctionMetadataServiceImpl
import com.kartverket.microsoft.MicrosoftService
import com.kartverket.microsoft.MicrosoftServiceImpl
import com.kartverket.plugins.configureCors
import com.kartverket.plugins.configureRouting
import com.kartverket.plugins.configureSerialization
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
        module = Application::module,
        environment = applicationEnvironment {
            config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
        },
        configure = {
            connector {
                port = 8080
                host = "0.0.0.0"
            }
        }
    ).start(wait = true)
}

fun CoroutineScope.launchCleanupJob(
    functionHistoryCleanupConfig: FunctionHistoryCleanupConfig,
    database: Database
): Job {
    val cleanupIntervalWeeks = functionHistoryCleanupConfig.cleanupIntervalWeeks
    val cleanupInterval: Duration = (cleanupIntervalWeeks * 7).days

    return launch(Dispatchers.IO) {
        while (isActive) {
            try {
                logger.info("Running scheduled cleanup every $cleanupIntervalWeeks weeks.")
                cleanupFunctionsHistory(functionHistoryCleanupConfig.deleteOlderThanDays, database)
            } catch (e: Exception) {
                logger.error("Error during function history cleanup: ${e.message}")
            }
            delay(cleanupInterval.inWholeMilliseconds)
        }
    }
}

fun cleanupFunctionsHistory(deleteOlderThanDays: Int, database: Database) {
    logger.info("Running scheduled cleanup for functions_history table. Deleting entries older than $deleteOlderThanDays days.")

    database.getConnection().use { conn ->
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
    val database = JDBCDatabase.create(config.databaseConfig)
    val microsoftService = MicrosoftServiceImpl.load(config.entraConfig)
    val functionService = FunctionServiceImpl(database)
    val functionMetadataService = FunctionMetadataServiceImpl(database, microsoftService)
    val authService = AuthServiceImpl(config.authConfig.superUserGroupId, functionMetadataService, microsoftService)
    val dataDumpService = DataDumpServiceImpl(database)
    configureAPILayer(config, authService, functionService, functionMetadataService, microsoftService, dataDumpService)
    launchCleanupJob(config.functionHistoryCleanup, database)

    monitor.subscribe(ApplicationStopped) {
        database.closePool()
    }
}

fun Application.configureAPILayer(
    config: AppConfig,
    authService: AuthService,
    functionService: FunctionService,
    functionMetadataService: FunctionMetadataService,
    microsoftService: MicrosoftService,
    dataDumpService: DataDumpService
) {
    configureSerialization()
    configureCors(config)
    configureAuth(config.authConfig)
    configureRouting(authService, functionService, functionMetadataService, microsoftService, dataDumpService)
}
