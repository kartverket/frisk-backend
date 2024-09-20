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
    val baseUrl = System.getenv("baseUrl")
    val tenantId = System.getenv("tenantId")
    val issuerPath = "/v2.0"
    val issuer = "$baseUrl/$tenantId$issuerPath"
    val clientId = System.getenv("clientId")
    val jwksPath = "/discovery/v2.0/keys"
    val jwksUri = "$baseUrl/$tenantId$jwksPath"

    logger.info("JWT URI: $jwksUri")

    val jwkProvider =
        JwkProviderBuilder(URL(jwksUri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(jwkProvider, issuer) {
                logger.info("JWT issuer:  $issuer")
                withIssuer(issuer)
                acceptLeeway(3)
                withAudience(clientId)
            }

            validate { jwtCredential ->
                logger.info("Validating JWT Credential: ${jwtCredential.payload}")
                if (jwtCredential.audience.contains(clientId)) {
                    logger.info("JWT Credential is valid")
                    JWTPrincipal(jwtCredential.payload)
                } else {
                    logger.info("JWT Credential is invalid: Audience mismatch")
                    null
                }
            }
        }
    }
}