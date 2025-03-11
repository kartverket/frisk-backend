package com.kartverket.microsoft

import com.kartverket.auth.getUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.microsoftRoutes(microsoftService: MicrosoftService) {
    route("/microsoft") {
        route("/me") {
            route("/teams") {
                get {
                    val userId = call.getUserId() ?: run {
                        call.respond(HttpStatusCode.Forbidden)
                        return@get
                    }
                    val groups = microsoftService.getMemberGroups(userId.value)
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
                    val group = microsoftService.getGroup(groupId)
                    call.respond(group)
                }
            }
        }
    }
}