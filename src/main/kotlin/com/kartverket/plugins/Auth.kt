package com.kartverket.plugins
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.logging.*
import java.net.URL
import java.util.concurrent.TimeUnit

val logger = KtorSimpleLogger("FunctionRoutes")

fun Application.configureAuth() {
    val tenantId = System.getenv("tenantId")
    val clientId = System.getenv("clientId")
    val jwksUri = "https://login.microsoftonline.com/$tenantId/discovery/v2.0/keys"
    val issuer = "https://login.microsoftonline.com/$tenantId/v2.0"

    logger.info("JWT URI: $jwksUri")
    logger.info("issuer: $issuer")

    val jwkProvider =
        JwkProviderBuilder(URL(jwksUri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    install(Authentication) {
        jwt("auth-jwt") {
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