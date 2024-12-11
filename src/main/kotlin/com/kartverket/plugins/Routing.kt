package com.kartverket.plugins

import com.kartverket.Database
import com.kartverket.functions.functionRoutes
import com.kartverket.functions.metadata.functionMetadataRoutes
import com.kartverket.microsoft.microsoftRoutes
import com.kartverket.toCsv
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
            functionMetadataRoutes()
            microsoftRoutes()
            get("/dump") {
                if (!call.hasSuperUserAccess()) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }
                val fileName = "data.csv"
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
                )

                call.respondBytes(
                    bytes = Database.getDump().toCsv().toByteArray(Charsets.UTF_8),
                    contentType = ContentType.Text.CSV.withCharset(Charsets.UTF_8)
                )
            }
        }
        route("/health") {
            get {
                call.respondText("Up and running!", ContentType.Text.Plain)
            }
        }
    }
}
