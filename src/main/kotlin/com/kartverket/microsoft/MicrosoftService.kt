package com.kartverket.microsoft

import com.azure.identity.ClientSecretCredentialBuilder
import com.kartverket.configuration.EntraConfig
import com.microsoft.graph.models.Group
import com.microsoft.graph.serviceclient.GraphServiceClient
import kotlinx.serialization.Serializable

@Serializable
data class TeamDTO(
    val id: String,
    val displayName: String,
)

interface MicrosoftService {
    fun getMemberGroups(userId: String): List<TeamDTO>
    fun getGroup(groupId: String): TeamDTO
}

class MicrosoftServiceImpl(
    private val graphClient: GraphServiceClient
) : MicrosoftService {
    override fun getMemberGroups(userId: String): List<TeamDTO> {
        val groups = graphClient.users().byUserId(userId).memberOf().graphGroup().get().value

        return groups.map {
            it.toTeamDTO()
        }
    }

    override fun getGroup(groupId: String): TeamDTO {
        return graphClient.groups().byGroupId(groupId).get().toTeamDTO()
    }

    companion object {
        fun load(config: EntraConfig): MicrosoftService {
            val scopes = "https://graph.microsoft.com/.default"
            val credential = ClientSecretCredentialBuilder()
                .clientId(config.clientId)
                .tenantId(config.tenantId)
                .clientSecret(config.clientSecret)
                .build()
            val graphClient = GraphServiceClient(credential, scopes)

            return MicrosoftServiceImpl(graphClient)
        }
    }

}

fun Group.toTeamDTO(): TeamDTO {
    return TeamDTO(
        id = this.id,
        displayName = this.displayName,
    )
}