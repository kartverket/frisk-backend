package com.kartverket.auth

import com.kartverket.functions.metadata.FunctionMetadataService
import com.kartverket.microsoft.MicrosoftService

interface AuthService {
    fun hasMetadataAccess(userId: UserId,  metadataId: Int): Boolean
    fun hasFunctionAccess(userId: UserId,  functionId: Int): Boolean
    fun hasSuperUserAccess(userId: UserId): Boolean
}

class AuthServiceImpl(
    private val superUserGroupId: String?,
) : AuthService {
    override fun hasMetadataAccess(userId: UserId, metadataId: Int): Boolean {
        val metadata = FunctionMetadataService.getFunctionMetadataById(metadataId) ?: run {
            return false
        }
        return hasFunctionAccess(userId, metadata.functionId)
    }

    override fun hasSuperUserAccess(userId: UserId): Boolean {
        if (superUserGroupId == null) return false
        return hasTeamAccess(userId, superUserGroupId)
    }

    override fun hasFunctionAccess(userId: UserId, functionId: Int): Boolean {
        val functionTeams = FunctionMetadataService.getFunctionMetadata(functionId, "team", null)

        return if (functionTeams.isEmpty()) {
            true
        } else {
            functionTeams.any { hasTeamAccess(userId, it.value) }
        }
    }

    private fun hasTeamAccess(userId: UserId, teamId: String): Boolean {
        val userTeams = MicrosoftService.getMemberGroups(userId.value)
        return userTeams.any { it.id == teamId }
    }
}