package com.kartverket.auth
import com.auth0.jwk.JwkProviderBuilder
import com.kartverket.configuration.AuthConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.logging.*
import java.net.URI
import java.util.concurrent.TimeUnit

const val AUTH_JWT = "auth-jwt"

val logger = KtorSimpleLogger("FunctionRoutes")

fun Application.configureAuth(authConfig: AuthConfig) {

    logger.info("JWT URI: ${authConfig.jwksUri}")
    logger.info("issuer: ${authConfig.issuer}")

    val jwkProvider =
        JwkProviderBuilder(URI(authConfig.jwksUri).toURL())
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = "Frisk Backend"
            verifier(jwkProvider, authConfig.issuer) {
                withIssuer(authConfig.issuer)
                withAudience(authConfig.clientId)
                acceptLeeway(3)
            }

            validate { jwtCredential ->
                logger.info("Validating JWT Credential: ${jwtCredential.payload.issuer}")
                JWTPrincipal(jwtCredential.payload)
            }
        }
    }
}

@JvmInline
value class UserId(val value: String)

fun ApplicationCall.getUserId(): UserId? {
    val userid = this.principal<JWTPrincipal>()?.payload?.getClaim("oid")?.asString() ?: return null
    return UserId(userid)
}
