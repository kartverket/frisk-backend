package com.kartverket.plugins

import com.kartverket.functions.dependencies.functionDependenciesRoutes
import com.kartverket.functions.functionRoutes
import com.kartverket.functions.metadata.functionMetadataRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        authenticate("auth-jwt") {
            functionRoutes()
            functionDependenciesRoutes()

            // some of these routes shall be protected by another type of auth
            functionMetadataRoutes()
        }
    }
}
