package com.kartverket

import com.kartverket.plugins.configureAuth
import com.kartverket.plugins.configureCors
import com.kartverket.plugins.configureRouting
import com.kartverket.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config
    Database.initDatabase(config)
    Database.migrate(config)

    configureSerialization()
    configureCors()
    configureAuth()
    configureRouting()

    environment.monitor.subscribe(ApplicationStopped) {
        Database.closePool()
    }
}
