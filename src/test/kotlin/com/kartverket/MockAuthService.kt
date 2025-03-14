package com.kartverket

import com.kartverket.auth.AuthService
import io.ktor.server.application.*

interface MockAuthService : AuthService {
    override fun hasMetadataAccess(call: ApplicationCall, metadataId: Int): Boolean = TODO("Not yet implemented")
    override fun hasFunctionAccess(call: ApplicationCall, functionId: Int): Boolean = TODO("Not yet implemented")
    override fun hasSuperUserAccess(call: ApplicationCall): Boolean = TODO("Not yet implemented")

}