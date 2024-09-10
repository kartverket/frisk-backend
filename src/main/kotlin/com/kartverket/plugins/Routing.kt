package com.kartverket.plugins

import com.kartverket.functions.functionRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        functionRoutes()
    }
}