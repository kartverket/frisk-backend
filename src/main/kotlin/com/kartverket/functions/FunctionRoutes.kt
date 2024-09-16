package com.kartverket.functions

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.functionRoutes() {
    authenticate("auth-bearer") {
        route("/functions") {
            get {
                val search = call.request.queryParameters["search"]
                val funcs = FunctionService.getFunctions(search)
                call.respond(funcs)
            }
            post {
                val newFunction = call.receive<CreateFunctionDto>()
                val f = FunctionService.createFunction(newFunction) ?: run {
                    call.respond(HttpStatusCode.InternalServerError)
                    return@post
                }
                call.respond(f)
            }
            route("/{id}") {
                get {
                    val id = call.parameters["id"]?.toInt() ?: run {
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
                    val id = call.parameters["id"]?.toInt() ?: run {
                        call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                        return@delete
                    }
                    FunctionService.deleteFunction(id)
                    call.respond(HttpStatusCode.NoContent)
                }
                get("/children") {
                    val id = call.parameters["id"]?.toInt() ?: run {
                        call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                        return@get
                    }
                    val children = FunctionService.getChildren(id)
                    call.respond(children)
                }

                route("/dependencies") {
                    post {
                        val newDependency = call.receive<FunctionDependency>()
                        val dep = FunctionDependencyService.createFunctionDependency(newDependency) ?: run {
                            call.respond(HttpStatusCode.InternalServerError)
                            return@post
                        }
                        call.respond(dep)
                    }

                    delete("/{dependencyId}") {
                        val id = call.parameters["id"]?.toInt() ?: run {
                            call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                            return@delete
                        }
                        val depId = call.parameters["dependencyId"]?.toInt() ?: run {
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
                        val id = call.parameters["id"]?.toInt() ?: run {
                            call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                            return@get
                        }
                        val deps = FunctionDependencyService.getFunctionDependencies(id)
                        call.respond(deps)
                    }
                }

                get("/dependents") {
                    val id = call.parameters["id"]?.toInt() ?: run {
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