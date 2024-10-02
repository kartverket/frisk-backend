package com.kartverket.microsoft

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.models.Group
import com.microsoft.graph.serviceclient.GraphServiceClient
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable

val logger = KtorSimpleLogger("FunctionRoutes")

@Serializable
data class GroupDTO(
    val id: String,
    val displayName: String,
)

object MicrosoftService {
    private val tenantId = System.getenv("tenantId")
    private val clientId = System.getenv("clientId")
    private val clientSecret = System.getenv("clientSecret")

    private val scopes = "https://graph.microsoft.com/.default"
    private val credential = ClientSecretCredentialBuilder().clientId(clientId).tenantId(tenantId).clientSecret(clientSecret).build()
    private val graphClient = GraphServiceClient(credential, scopes)

    fun getMemberGroups(userId: String): List<GroupDTO> {
        val groups = graphClient.users().byUserId(userId).memberOf().graphGroup().get().value

        return groups.map {
            it.toDTO()
        }
    }

    fun getGroup(groupId: String): GroupDTO {
        return graphClient.groups().byGroupId(groupId).get().toDTO()
    }
}

fun Group.toDTO(): GroupDTO {
    return GroupDTO(
        id = this.id,
        displayName = this.displayName,
    )
}