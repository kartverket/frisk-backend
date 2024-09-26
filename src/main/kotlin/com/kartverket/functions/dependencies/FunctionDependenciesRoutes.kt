package com.kartverket.functions.dependencies

import com.kartverket.functions.logger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.functionDependenciesRoutes() {
    route("/functions") {
        route("/{id}") {
            route("/dependencies") {
                post {
                    logger.info("Received post on /functions/{id}/dependencies")
                    val newDependency = call.receive<FunctionDependency>()
                    val dep =
                        FunctionDependencyService.createFunctionDependency(newDependency) ?: run {
                            logger.error("Invalid input")
                            call.respond(HttpStatusCode.InternalServerError)
                            return@post
                        }
                    call.respond(dep)
                }

                delete("/{dependencyId}") {
                    logger.info("Received delete on /functions/{id}/dependencies/{dependencyId}")
                    val id =
                        call.parameters["id"]?.toInt() ?: run {
                            logger.error("Invalid id parameter")
                            call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                            return@delete
                        }
                    val depId =
                        call.parameters["dependencyId"]?.toInt() ?: run {
                            logger.error("Invalid id parameter")
                            call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                            return@delete
                        }

                    val depToDelete =
                        FunctionDependency(
                            functionId = id,
                            dependencyFunctionId = depId,
                        )
                    FunctionDependencyService.deleteFunctionDependency(depToDelete)
                    call.respond(HttpStatusCode.NoContent)
                }

                get {
                    logger.info("Received get on /functions/{id}/dependencies/")
                    val id =
                        call.parameters["id"]?.toInt() ?: run {
                            logger.error("Invalid id parameter")
                            call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                            return@get
                        }
                    val deps = FunctionDependencyService.getFunctionDependencies(id)
                    call.respond(deps)
                }
            }

            route("/dependents") {
                get {
                    logger.info("Received get on /functions/{id}/dependendents/")
                    val id =
                        call.parameters["id"]?.toInt() ?: run {
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
}