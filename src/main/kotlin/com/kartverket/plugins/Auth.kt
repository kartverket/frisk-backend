package com.kartverket.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureAuth() {
    install(Authentication) {
        bearer("auth-bearer") {
            authenticate { tokenCredential ->
                if (tokenCredential.token == "test123") {
                    UserIdPrincipal("frisk")
                } else {
                    null
                }
            }
        }
    }
}