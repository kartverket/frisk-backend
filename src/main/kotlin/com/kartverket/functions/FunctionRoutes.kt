package com.kartverket.functions

import com.kartverket.functions.metadata.FunctionMetadataService
import com.kartverket.plugins.hasFunctionAccess
import com.kartverket.plugins.hasSuperUserAccess
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.KtorSimpleLogger

val logger = KtorSimpleLogger("FunctionRoutes")

fun Route.functionRoutes() {
    route("/functions") {
        get {
            logger.info("Received get on /functions")
            val search = call.request.queryParameters["search"]
            val funcs = FunctionService.getFunctions(search)
            call.respond(funcs)
        }
        post {
            logger.info("Received post on /functions")
            val newFunction = call.receive<CreateFunctionWithMetadataDto>()

            if (newFunction.function.name.isBlank() || newFunction.function.parentId <= 0) {
                logger.error("Invalid function input")
                call.respond(HttpStatusCode.BadRequest, "Function name and parent ID are required")
                return@post
            }
            val f =
                FunctionService.createFunction(newFunction.function) ?: run {
                    logger.error("Function creation failed")
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create function")
                    return@post
                }
            if (newFunction.metadata.isNotEmpty()) {
                newFunction.metadata.forEach { m ->
                    FunctionMetadataService.addMetadataToFunction(f.id, m)
                }
            }
            call.respond(f)
        }
        route("/{id}") {
            get {
                logger.info("Received get on /functions/{id}")
                val id =
                    call.parameters["id"]?.toIntOrNull() ?: run {
                        logger.error("Invalid id parameter: ${call.parameters["id"]}")
                        call.respond(HttpStatusCode.BadRequest, "You have to supply a valid integer id")
                        return@get
                    }
                val f =
                    FunctionService.getFunction(id) ?: run {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }
                call.respond(f)
            }
            put {
                logger.info("Received put on /functions/{id}")
                val id =
                    call.parameters["id"]?.toIntOrNull() ?: run {
                        logger.error("Invalid id parameter: ${call.parameters["id"]}")
                        call.respond(HttpStatusCode.BadRequest, "You have to supply a valid integer id")
                        return@put
                    }

                if (!call.hasFunctionAccess(id) && !call.hasSuperUserAccess()) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@put
                }


                val updatedFunction = call.receive<UpdateFunctionDto>()
                val f =
                    FunctionService.updateFunction(id, updatedFunction) ?: run {
                        call.respond(HttpStatusCode.NotFound)
                        return@put
                    }
                call.respond(f)
            }
            delete {
                logger.info("Received delete on /functions/{id}")
                val id =
                    call.parameters["id"]?.toIntOrNull() ?: run {
                        logger.error("Invalid id parameter: ${call.parameters["id"]}")
                        call.respond(HttpStatusCode.BadRequest, "You have to supply a valid integer id")
                        return@delete
                    }

                if (!call.hasFunctionAccess(id) && !call.hasSuperUserAccess()) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@delete
                }

                FunctionService.deleteFunction(id)
                call.respond(HttpStatusCode.NoContent)
            }
            get("/children") {
                logger.info("Received get request on functions/{id}/childeren")
                val id =
                    call.parameters["id"]?.toIntOrNull() ?: run {
                        logger.error("Invalid id parameter: ${call.parameters["id"]}")
                        call.respond(HttpStatusCode.BadRequest, "You have to supply a valid integer id")
                        return@get
                    }
                val children = FunctionService.getChildren(id)
                call.respond(children)
            }
            get("/access") {
                logger.info("Received get request on functions/{id}/access")
                val id = call.parameters["id"]?.toInt() ?: run {
                    logger.error("Invalid id parameter")
                    call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                    return@get
                }

                val hasAccess = call.hasFunctionAccess(id) || call.hasSuperUserAccess()
                call.respond(hasAccess)
            }
        }
    }
}
