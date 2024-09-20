package com.kartverket.functions

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.KtorSimpleLogger

val logger = KtorSimpleLogger("FunctionRoutes")

fun Route.functionRoutes() {

        route("/functions") {
            get {
                logger.info("Received get on /functions")
                val principal = call.principal<JWTPrincipal>()
                val search = call.request.queryParameters["search"]
                val funcs = FunctionService.getFunctions(search)
                call.respond(funcs)
            }
            post {
                logger.info("Received post on /functions")
                val newFunction = call.receive<CreateFunctionDto>()
                val f = FunctionService.createFunction(newFunction) ?: run {
                    logger.error("Invalid input")
                    call.respond(HttpStatusCode.InternalServerError)
                    return@post
                }
                call.respond(f)
            }
            route("/{id}") {
                get {
                    logger.info("Received get on /functions/{id}")
                    val id = call.parameters["id"]?.toInt() ?: run {
                        logger.error("Invalid id parameter")
                        call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                        return@get
                    }
                    val f = FunctionService.getFunction(id) ?: run {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }
                    call.respond(f)
                }
                delete {
                    logger.info("Received delete on /functions/{id}")
                    val id = call.parameters["id"]?.toInt() ?: run {
                        logger.error("Invalid id parameter")
                        call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                        return@delete
                    }
                    FunctionService.deleteFunction(id)
                    call.respond(HttpStatusCode.NoContent)
                }
                get("/children") {
                    logger.info("Received get request on functions/{id}/childeren")
                    val id = call.parameters["id"]?.toInt() ?: run {
                        logger.error("Invalid id parameter")
                        call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                        return@get
                    }
                    val children = FunctionService.getChildren(id)
                    call.respond(children)
                }

                route("/dependencies") {
                    post {
                        logger.info("Received post on /functions/{id}/dependencies")
                        val newDependency = call.receive<FunctionDependency>()
                        val dep = FunctionDependencyService.createFunctionDependency(newDependency) ?: run {
                            logger.error("Invalid input")
                            call.respond(HttpStatusCode.InternalServerError)
                            return@post
                        }
                        call.respond(dep)
                    }

                    delete("/{dependencyId}") {
                        logger.info("Received delete on /functions/{id}/dependencies/{dependencyId}")
                        val id = call.parameters["id"]?.toInt() ?: run {
                            logger.error("Invalid id parameter")
                            call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                            return@delete
                        }
                        val depId = call.parameters["dependencyId"]?.toInt() ?: run {
                            logger.error("Invalid id parameter")
                            call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                            return@delete
                        }

                        val depToDelete = FunctionDependency(
                            functionId = id,
                            dependencyFunctionId = depId
                        )
                        FunctionDependencyService.deleteFunctionDependency(depToDelete)
                        call.respond(HttpStatusCode.NoContent)
                    }

                    get {
                        logger.info("Received get on /functions/{id}/dependencies/")
                        val id = call.parameters["id"]?.toInt() ?: run {
                            logger.error("Invalid id parameter")
                            call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                            return@get
                        }
                        val deps = FunctionDependencyService.getFunctionDependencies(id)
                        call.respond(deps)
                    }
                }

                get("/dependents") {
                    logger.info("Received get on /functions/{id}/dependendents/")
                    val id = call.parameters["id"]?.toInt() ?: run {
                        logger.error("Invalid id parameter")
                        call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                        return@get
                    }
                val deps = FunctionDependencyService.getFunctionDependents(id)
                    call.respond(deps)
            }
            }
        }
    
}