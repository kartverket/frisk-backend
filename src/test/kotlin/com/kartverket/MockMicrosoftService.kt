package com.kartverket

import com.kartverket.microsoft.MicrosoftService
import com.kartverket.microsoft.TeamDTO

interface MockMicrosoftService : MicrosoftService {
    override fun getMemberGroups(userId: String): List<TeamDTO> = TODO("Not yet implemented")
    override fun getGroup(groupId: String): TeamDTO = TODO("Not yet implemented")
}