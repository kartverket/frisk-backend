package com.kartverket.functions.metadata

import com.kartverket.plugins.hasFunctionAccess
import com.kartverket.plugins.hasMetadataAccess
import com.kartverket.plugins.hasSuperUserAccess
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*

val logger = KtorSimpleLogger("FunctionRoutes")



fun Route.functionMetadataRoutes() {
    route("/functions") {
        route("/{id}") {
            route("/metadata") {
                get {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        logger.warn("Invalid id parameter in Received get request on functions/{id}/metadata")
                        call.respond(HttpStatusCode.BadRequest, "Invalid function id!")
                        return@get
                    }
                    val metadata = FunctionMetadataService.getFunctionMetadata(id, null, null)
                    call.respond(metadata)
                }
                post {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        logger.warn("Invalid id parameter in Received post request on functions/{id}/metadata")
                        call.respond(HttpStatusCode.BadRequest, "Invalid function id!")
                        return@post
                    }

                    if (!call.hasFunctionAccess(id)) {
                        logger.warn("Forbidden access attempt: post request on functions/{id}/metadata")
                        call.respond(HttpStatusCode.Forbidden)
                        return@post
                    }

                    val metadata = call.receive<CreateFunctionMetadataDTO>()
                    FunctionMetadataService.addMetadataToFunction(id, metadata)
                    call.respond(HttpStatusCode.NoContent)
                }
                get("/access") {
                    val id = call.parameters["id"]?.toInt() ?: run {
                        call.respond(HttpStatusCode.BadRequest, "Invalid function id")
                        return@get
                    }

                    val hasAccess = call.hasFunctionAccess(id) || call.hasSuperUserAccess()
                    call.respond(hasAccess)
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
        get("indicator") {
            val functionId = call.request.queryParameters["functionId"]?.toIntOrNull() ?: run {
                logger.warn("Bad request: Invalid or missing 'functionId' parameter on /indicator")
                throw BadRequestException("Invalid function key!")
            }
            val key = call.request.queryParameters["key"] ?: run{
                logger.warn("Bad request: Invalid or missing 'key' parameter on /indicator")
                throw BadRequestException("Invalid function key!")
            }
            val value = call.request.queryParameters["value"]

            val functions = FunctionMetadataService.getIndicators(key, value, functionId)
            call.respond(functions)
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
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    logger.error("Invalid id parameter in patch request on metadata/{id}")
                    call.respond(HttpStatusCode.BadRequest, "Invalid metadata id!")
                    return@patch
                }
                val updatedMetadata = call.receive<UpdateFunctionMetadataDTO>()

                if (!call.hasMetadataAccess(id)) {
                    logger.warn("Forbidden access attempt: patch request on metadata/{id}")
                    call.respond(HttpStatusCode.Forbidden)
                    return@patch
                }

                FunctionMetadataService.updateMetadataValue(id, updatedMetadata)
                call.respond(HttpStatusCode.NoContent)
            }
            delete {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    logger.error("Invalid id parameter in delete request on metadata/{id}")
                    call.respond(HttpStatusCode.BadRequest, "Invalid metadata id!")
                    return@delete
                }

                if (!call.hasMetadataAccess(id)) {
                    logger.warn("Forbidden access attempt: delete request on metadata/{id}")
                    call.respond(HttpStatusCode.Forbidden)
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