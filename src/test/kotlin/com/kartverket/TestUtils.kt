package com.kartverket

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.kartverket.functions.CreateFunctionDto
import com.kartverket.functions.CreateFunctionWithMetadataDto
import com.kartverket.functions.Function
import com.kartverket.functions.metadata.CreateFunctionMetadataDTO
import com.kartverket.plugins.AUTH_JWT
import com.kartverket.plugins.configureRouting
import com.kartverket.plugins.configureSerialization
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*
import kotlin.test.assertEquals

object TestUtils {

    private var isDatabaseInitialized = false
    val postgresContainer = PostgreSQLContainer("postgres:15-alpine").apply {
        start()
    }

    fun Application.testModule() {
        setupTestDatabase()
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
        configureRouting()
    }

    fun generateTestToken(): String {
        return JWT.create()
            .withAudience("test-audience")
            .withIssuer("test-issuer")
            .withClaim("oid", "test-user-id")
            .sign(Algorithm.HMAC256("test-secret"))
    }

    private fun setupTestDatabase() {
        if (isDatabaseInitialized) return
        isDatabaseInitialized = true

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = postgresContainer.jdbcUrl
            username = postgresContainer.username
            password = postgresContainer.password
            driverClassName = "org.postgresql.Driver"
        }
        Database.dataSource = HikariDataSource(hikariConfig)
        val flyway = Flyway.configure()
            .validateMigrationNaming(true)
            .createSchemas(true)
            .dataSource(Database.dataSource)
            .locations("classpath:db/migration")
            .load()

        flyway.migrate()
    }

    fun stopTestDatabase() {
        postgresContainer.stop()
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