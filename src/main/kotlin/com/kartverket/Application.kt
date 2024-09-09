package com.kartverket

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config
    Database.initDatabase(config)
    Database.migrate(config)

    configureSerialization()
    configureAuth()
    configureRouting()

    environment.monitor.subscribe(ApplicationStopped) {
        Database.closePool()
    }
}
