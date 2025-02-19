package com.kartverket.plugins
import com.auth0.jwk.JwkProviderBuilder
import com.kartverket.functions.metadata.FunctionMetadataService
import com.kartverket.microsoft.MicrosoftService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.logging.*
import java.net.URI
import java.util.concurrent.TimeUnit

const val AUTH_JWT = "auth-jwt"

val logger = KtorSimpleLogger("FunctionRoutes")

fun Application.configureAuth() {
    val tenantId = System.getenv("tenantId")
    val clientId = System.getenv("clientId")
    val jwksUri = "https://login.microsoftonline.com/$tenantId/discovery/v2.0/keys"
    val issuer = "https://login.microsoftonline.com/$tenantId/v2.0"

    logger.info("JWT URI: $jwksUri")
    logger.info("issuer: $issuer")

    val jwkProvider =
        JwkProviderBuilder(URI(jwksUri).toURL())
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = "Frisk Backend"
            verifier(jwkProvider, issuer) {
                withIssuer(issuer)
                withAudience(clientId)
                acceptLeeway(3)
            }

            validate { jwtCredential ->
                logger.info("Validating JWT Credential: ${jwtCredential.payload.issuer}")
                JWTPrincipal(jwtCredential.payload)
            }
        }
    }
}

fun JWTPayloadHolder.getUserId(): String {
    return this.payload.getClaim("oid").asString()
}

fun hasMetadataAccess(userId: String, metadataId: Int): Boolean {
    val metadata = FunctionMetadataService.getFunctionMetadataById(metadataId) ?: run {
        return false
    }
    return hasFunctionAccess(userId, metadata.functionId)
}

fun ApplicationCall.hasMetadataAccess(metadataId: Int): Boolean {
    val userId = this.getUserId() ?: run {
        return false
    }
    val metadata = FunctionMetadataService.getFunctionMetadataById(metadataId) ?: run {
        return false
    }
    return hasMetadataAccess(userId, metadata.id)
}

fun hasFunctionAccess(userId: String, functionId: Int): Boolean {
    val functionTeams = FunctionMetadataService.getFunctionMetadata(functionId, "team", null)

    return if (functionTeams.isEmpty()) {
        true
    } else {
        functionTeams.any { hasTeamAccess(userId, it.value) }
    }
}

fun ApplicationCall.hasFunctionAccess(functionId: Int): Boolean {
    val userId = this.getUserId() ?: run {
        return false
    }
    return hasFunctionAccess(userId, functionId)
}

fun hasTeamAccess(userId: String, teamId: String): Boolean {
    val userTeams = MicrosoftService.getMemberGroups(userId)
    return userTeams.any { it.id == teamId }
}

fun hasSuperUserAccess(userId: String): Boolean {
    val superUserGroupId = System.getenv("SUPER_USER_GROUP_ID") ?: return false
    return hasTeamAccess(userId, superUserGroupId)
}

fun ApplicationCall.hasSuperUserAccess(): Boolean {
    val userId = this.getUserId() ?: return false
    return hasSuperUserAccess(userId)
}

fun ApplicationCall.getUserId(): String? {
    return this.principal<JWTPrincipal>()?.getUserId()
}