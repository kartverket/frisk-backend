package com.kartverket.functions

import com.kartverket.auth.AuthService
import com.kartverket.functions.dto.CreateFunctionWithMetadataDto
import com.kartverket.functions.dto.UpdateFunctionDto
import com.kartverket.functions.metadata.FunctionMetadataService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.KtorSimpleLogger

val logger = KtorSimpleLogger("FunctionRoutes")

fun Route.functionRoutes(
    authService: AuthService,
    functionService: FunctionService,
    functionMetadataService: FunctionMetadataService
) {
    route("/functions") {
        get {
            logger.info("Received get on /functions")
            val search = call.request.queryParameters["search"]
            val funcs = functionService.getFunctions(search)
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
            val function = functionService.createFunction(newFunction.function) ?: run {
                logger.error("Function creation failed")
                call.respond(HttpStatusCode.InternalServerError, "Failed to create function")
                return@post
            }
            if (newFunction.metadata.isNotEmpty()) {
                newFunction.metadata.forEach { m ->
                    functionMetadataService.addMetadataToFunction(function.id, m)
                }
            }
            call.respond(function)
        }
        route("/{id}") {
            get {
                logger.info("Received get on /functions/{id}")
                val id = call.parameters["id"]?.toIntOrNull() ?: run {
                    logger.warn("Invalid id parameter: ${call.parameters["id"]}")
                    call.respond(HttpStatusCode.BadRequest, "You have to supply a valid integer id")
                    return@get
                }
                val function = functionService.getFunction(id) ?: run {
                    logger.warn("Function not found")
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(function)
            }
            put {
                logger.info("Received put on /functions/{id}")
                val id = call.parameters["id"]?.toIntOrNull() ?: run {
                    logger.warn("Invalid id parameter: ${call.parameters["id"]}")
                    call.respond(HttpStatusCode.BadRequest, "You have to supply a valid integer id")
                    return@put
                }

                if (!authService.hasFunctionAccess(call, id) && !authService.hasSuperUserAccess(call)) {
                    logger.warn("Forbidden access attempt")
                    call.respond(HttpStatusCode.Forbidden)
                    return@put
                }


                val updatedFunction = call.receive<UpdateFunctionDto>()
                val function = functionService.updateFunction(id, updatedFunction) ?: run {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                call.respond(function)
            }
            delete {
                logger.info("Received delete on /functions/{id}")
                val id = call.parameters["id"]?.toIntOrNull() ?: run {
                    logger.warn("Invalid id parameter: ${call.parameters["id"]}")
                    call.respond(HttpStatusCode.BadRequest, "You have to supply a valid integer id")
                    return@delete
                }

                if (!authService.hasFunctionAccess(call, id) && !authService.hasSuperUserAccess(call)) {
                    logger.warn("Forbidden access attempt")
                    call.respond(HttpStatusCode.Forbidden)
                    return@delete
                }

                functionService.deleteFunction(id)
                call.respond(HttpStatusCode.NoContent)
            }
            get("/children") {
                logger.info("Received get request on functions/{id}/childeren")
                val id = call.parameters["id"]?.toIntOrNull() ?: run {
                    logger.warn("Invalid id parameter: ${call.parameters["id"]}")
                    call.respond(HttpStatusCode.BadRequest, "You have to supply a valid integer id")
                    return@get
                }
                val children = functionService.getChildren(id)
                call.respond(children)
            }
            get("/access") {
                logger.info("Received get request on functions/{id}/access")
                val id = call.parameters["id"]?.toInt() ?: run {
                    logger.error("Invalid id parameter")
                    call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                    return@get
                }

                val hasAccess = authService.hasFunctionAccess(call, id) || authService.hasSuperUserAccess(call)
                call.respond(hasAccess)
            }
        }
    }
}
