package com.kartverket

import com.kartverket.plugins.configureAuth
import com.kartverket.plugins.configureCors
import com.kartverket.plugins.configureRouting
import com.kartverket.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.logging.*

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

    environment.monitor.subscribe(ApplicationStopped) {
        Database.closePool()
    }
}
