package com.kartverket.auth

import com.kartverket.functions.metadata.FunctionMetadataService
import com.kartverket.microsoft.MicrosoftService
import io.ktor.server.application.*

interface AuthService {
    fun hasMetadataAccess(call: ApplicationCall, metadataId: Int): Boolean
    fun hasFunctionAccess(call: ApplicationCall, functionId: Int): Boolean
    fun hasSuperUserAccess(call: ApplicationCall): Boolean
}

class AuthServiceImpl(
    private val functionMetadataService: FunctionMetadataService,
) : AuthService {
    private fun hasMetadataAccess(userId: String, metadataId: Int): Boolean {
        val metadata = functionMetadataService.getFunctionMetadataById(metadataId) ?: run {
            return false
        }
        return hasFunctionAccess(userId, metadata.functionId)
    }

    override fun hasMetadataAccess(call: ApplicationCall, metadataId: Int): Boolean {
        val userId = call.getUserId() ?: run {
            return false
        }
        val metadata = functionMetadataService.getFunctionMetadataById(metadataId) ?: run {
            return false
        }
        return hasMetadataAccess(userId, metadata.id)
    }

    private fun hasFunctionAccess(userId: String, functionId: Int): Boolean {
        val functionTeams = functionMetadataService.getFunctionMetadata(functionId, "team", null)

        return if (functionTeams.isEmpty()) {
            true
        } else {
            functionTeams.any { hasTeamAccess(userId, it.value) }
        }
    }

    override fun hasFunctionAccess(call: ApplicationCall, functionId: Int): Boolean {
        val userId = call.getUserId() ?: return false
        return hasFunctionAccess(userId, functionId)
    }

    private fun hasTeamAccess(userId: String, teamId: String): Boolean {
        val userTeams = MicrosoftService.getMemberGroups(userId)
        return userTeams.any { it.id == teamId }
    }

    private fun hasSuperUserAccess(userId: String): Boolean {
        val superUserGroupId = System.getenv("SUPER_USER_GROUP_ID") ?: return false
        return hasTeamAccess(userId, superUserGroupId)
    }

    override fun hasSuperUserAccess(call: ApplicationCall): Boolean {
        val userId = call.getUserId() ?: return false
        return hasSuperUserAccess(userId)
    }
}