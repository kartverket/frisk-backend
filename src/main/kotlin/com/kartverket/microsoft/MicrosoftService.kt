package com.kartverket.microsoft

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.models.Group
import com.microsoft.graph.serviceclient.GraphServiceClient
import kotlinx.serialization.Serializable

@Serializable
data class TeamDTO(
    val id: String,
    val displayName: String,
)

object MicrosoftService {
    private val tenantId = System.getenv("tenantId")
    private val clientId = System.getenv("clientId")
    private val clientSecret = System.getenv("CLIENT_SECRET")

    private val scopes = "https://graph.microsoft.com/.default"
    private val credential = ClientSecretCredentialBuilder().clientId(clientId).tenantId(tenantId).clientSecret(clientSecret).build()
    private val graphClient = GraphServiceClient(credential, scopes)

    fun getMemberGroups(userId: String): List<TeamDTO> {
        val groups = graphClient.users().byUserId(userId).memberOf().graphGroup().get().value

        return groups.map {
            it.toTeamDTO()
        }
    }

    fun getGroup(groupId: String): TeamDTO {
        return graphClient.groups().byGroupId(groupId).get().toTeamDTO()
    }

    fun getUserEmail(userId: String): String {
        return graphClient.users().byUserId(userId).get().mail
    }
}

fun Group.toTeamDTO(): TeamDTO {
    return TeamDTO(
        id = this.id,
        displayName = this.displayName,
    )
}