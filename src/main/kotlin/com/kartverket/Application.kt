package com.kartverket

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config
    Database.initDatabase(config)
    Database.migrate(config)

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
    }

    configureSerialization()
    configureAuth()
    configureRouting()

    environment.monitor.subscribe(ApplicationStopped) {
        Database.closePool()
    }
}
