package com.kartverket.plugins

import com.kartverket.functions.dependencies.functionDependenciesRoutes
import com.kartverket.functions.functionRoutes
import com.kartverket.functions.metadata.functionMetadataRoutes
import com.kartverket.microsoft.microsoftRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*

fun Application.configureRouting() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        authenticate(AUTH_JWT, strategy = AuthenticationStrategy.Required) {
            functionRoutes()
            functionDependenciesRoutes()
            functionMetadataRoutes()
            microsoftRoutes()
        }
        route("/functions") {
            get("/health") {
                call.respondText("Up and running!", ContentType.Text.Plain)
            }
        }
    }
}
