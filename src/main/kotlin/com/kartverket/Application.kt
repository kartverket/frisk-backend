package com.kartverket

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config
    Database.initDatabase(config)
    Database.migrate(config)

    configureSerialization()
    configureRouting()

    environment.monitor.subscribe(ApplicationStopped) {
        Database.closePool()
    }
}
