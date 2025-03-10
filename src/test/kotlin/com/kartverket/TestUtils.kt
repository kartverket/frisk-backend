package com.kartverket

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.kartverket.functions.dto.CreateFunctionDto
import com.kartverket.functions.dto.CreateFunctionWithMetadataDto
import com.kartverket.functions.Function
import com.kartverket.functions.FunctionService
import com.kartverket.functions.FunctionServiceImpl
import com.kartverket.functions.metadata.dto.CreateFunctionMetadataDTO
import com.kartverket.functions.metadata.FunctionMetadataService
import com.kartverket.auth.AUTH_JWT
import com.kartverket.auth.AuthService
import com.kartverket.functions.metadata.FunctionMetadataServiceImpl
import com.kartverket.microsoft.MicrosoftService
import com.kartverket.plugins.configureRouting
import com.kartverket.plugins.configureSerialization
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.test.assertEquals

object TestUtils {
    fun Application.testModule(
        testDatabase: Database = object : MockDatabase {},
        authService: AuthService = object : MockAuthService {},
        microsoftService: MicrosoftService = object : MockMicrosoftService {},
        functionService: FunctionService = FunctionServiceImpl(testDatabase),
        functionMetadataService: FunctionMetadataService = FunctionMetadataServiceImpl(
            testDatabase,
            microsoftService
        ),
    ) {
        configureSerialization()

        install(Authentication) {
            jwt(AUTH_JWT) {
                realm = "test"
                verifier(
                    JWT
                        .require(Algorithm.HMAC256("test-secret"))
                        .withAudience("test-audience")
                        .withIssuer("test-issuer")
                        .build()
                )
                validate { credentials -> JWTPrincipal(credentials.payload) }
            }
        }

        configureRouting(testDatabase, authService, functionService, functionMetadataService, microsoftService)
    }

    fun generateTestToken(): String {
        return JWT.create()
            .withAudience("test-audience")
            .withIssuer("test-issuer")
            .withClaim("oid", "test-user-id")
            .sign(Algorithm.HMAC256("test-secret"))
    }

    suspend fun createFunction(client: HttpClient, parentId: Int): Function {
        val function = CreateFunctionDto(
            name = "${UUID.randomUUID()}",
            description = "desc",
            parentId = parentId
        )

        val request = CreateFunctionWithMetadataDto(
            function = function,
            metadata = emptyList()
        )

        val response = client.post("/functions") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        return Json.decodeFromString(response.bodyAsText())
    }

    suspend fun addMetadata(client: HttpClient, functionId: Int, key: String, value: String) {

        val request = CreateFunctionMetadataDTO(key = key, value = value)

        val response = client.post("/functions/$functionId/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }
}