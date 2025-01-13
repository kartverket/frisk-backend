package com.kartverket

import com.kartverket.plugins.*
import com.kartverket.util.NewSchemaMetadataMapper
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch

fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    Database.initDatabase()
    Database.migrate()

    configureSerialization()
    configureCors()
    configureAuth()
    configureRouting()
    launch {
        NewSchemaMetadataMapper().addNewSchemaMetadata()
    }

    environment.monitor.subscribe(ApplicationStopped) {
        Database.closePool()
    }
}
