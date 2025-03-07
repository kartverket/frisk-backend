package com.kartverket.plugins

import com.kartverket.DumpRow
import com.kartverket.Database
import com.kartverket.functions.functionRoutes
import com.kartverket.functions.metadata.functionMetadataRoutes
import com.kartverket.microsoft.microsoftRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*

fun Application.configureRouting(database: Database) {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        authenticate(AUTH_JWT, strategy = AuthenticationStrategy.Required) {
            functionRoutes()
            functionMetadataRoutes()
            microsoftRoutes()
            get("/dump") {
//                if (!call.hasSuperUserAccess()) {
//                    call.respond(HttpStatusCode.Forbidden)
//                    return@get
//                }
                val fileName = "data.csv"
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
                )

                call.respondBytes(
                    bytes = database.getDump().toCsv().toByteArray(Charsets.UTF_8),
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

fun List<DumpRow>.toCsv(): String {
    return buildString {
        appendLine("id,name,description,path,key,value")
        for (row in this@toCsv) {
            appendLine("\"${row.id}\",\"${row.name}\",\"${row.description}\",\"${row.path}\",\"${row.key}\",\"${row.value}\"")
        }
    }
}
