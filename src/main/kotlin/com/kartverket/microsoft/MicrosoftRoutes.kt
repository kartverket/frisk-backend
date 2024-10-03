package com.kartverket.microsoft

import com.kartverket.plugins.getUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.microsoftRoutes() {
    route("/microsoft") {
        route("/me") {
            route("/teams") {
                get {
                    val userId = call.principal<JWTPrincipal>()?.getUserId() ?: run {
                        call.respond(HttpStatusCode.Forbidden)
                        return@get
                    }
                    val groups = MicrosoftService.getMemberGroups(userId)
                    call.respond(groups)
                }
            }
        }
        route("/teams") {
            route("{id}") {
                get {
                    val groupId = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val group = MicrosoftService.getGroup(groupId)
                    call.respond(group)
                }
            }
        }
    }
}