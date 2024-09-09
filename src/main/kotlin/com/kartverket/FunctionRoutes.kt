package com.kartverket

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.functionRoutes() {
    route("/functions") {
        get {
            val funcs = FunctionService.getAllFunctions()
            call.respond(funcs)
        }
        post {
            val newFunction = call.receive<CreateFunctionDao>()
            FunctionService.createFunction(newFunction)
            call.respond(HttpStatusCode.NoContent)
        }
        route("/{id}") {
            get {
                val id = call.parameters["id"]?.toInt()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                    return@get
                }
                val f = FunctionService.getFunction(id)
                if (f == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(f)
            }
            patch {
                val id = call.parameters["id"]?.toInt()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                    return@patch
                }
                val updatedFunction = call.receive<UpdateFunctionDao>()
                FunctionService.updateFunction(id, updatedFunction)
                call.respond(HttpStatusCode.NoContent)

            }
            delete {
                val id = call.parameters["id"]?.toInt()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "You have to supply an id")
                    return@delete
                }
                FunctionService.deleteFunction(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}