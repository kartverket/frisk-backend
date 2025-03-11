package com.kartverket

import com.kartverket.auth.AuthService
import com.kartverket.auth.UserId
import io.ktor.server.application.*

interface MockAuthService : AuthService {
    override fun hasMetadataAccess(userId: UserId, metadataId: Int): Boolean = TODO("Not yet implemented")
    override fun hasFunctionAccess(userId: UserId, functionId: Int): Boolean = TODO("Not yet implemented")
    override fun hasSuperUserAccess(userId: UserId): Boolean = TODO("Not yet implemented")

}