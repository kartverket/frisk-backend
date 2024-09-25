package com.kartverket.functions.metadata

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.functionMetadataRoutes() {
    route("/functions") {
        route("/{id}") {
            route("/metadata") {
                get {
                    val id = call.parameters["id"]?.toInt()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid function id!")
                        return@get
                    }
                    val metadata = FunctionMetadataService.getFunctionMetadata(id, null, null)
                    call.respond(metadata)
                }
                post {
                    val id = call.parameters["id"]?.toInt()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid function id!")
                        return@post
                    }
                    val metadata = call.receive<CreateFunctionMetadataDTO>()
                    FunctionMetadataService.addMetadataToFunction(id, metadata)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
    route("/metadata") {
        get {
            val key = call.request.queryParameters["key"]
            val value = call.request.queryParameters["value"]
            val functionId = call.request.queryParameters["functionId"]?.toInt()

            val metadata = FunctionMetadataService.getFunctionMetadata(functionId, key, value)
            call.respond(metadata)
        }
        route("/keys") {
            get {
                val search = call.request.queryParameters["search"]
                call.respond(FunctionMetadataService.getFunctionMetadataKeys(search))
            }
        }
        route("/{id}") {
            get {
                call.respond(HttpStatusCode.NotImplemented)
            }
            patch {
                val id = call.parameters["id"]?.toInt()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid metadata id!")
                    return@patch
                }
                val updatedMetadata = call.receive<UpdateFunctionMetadataDTO>()

                FunctionMetadataService.updateMetadataValue(id, updatedMetadata)
                call.respond(HttpStatusCode.NoContent)
            }
            delete {
                val id = call.parameters["id"]?.toInt()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid metadata id!")
                    return@delete
                }
                FunctionMetadataService.deleteMetadata(id)
                call.respond(HttpStatusCode.NoContent)
            }
            route("/function") {
                get {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }
    }
}