package com.kartverket.integrationTests

import com.kartverket.JDBCDatabase
import com.kartverket.MockMicrosoftService
import com.kartverket.TestDatabase
import com.kartverket.TestUtils.generateTestToken
import com.kartverket.TestUtils.testModule
import com.kartverket.auth.AuthServiceImpl
import com.kartverket.functions.dto.CreateFunctionDto
import com.kartverket.functions.dto.CreateFunctionWithMetadataDto
import com.kartverket.functions.Function
import com.kartverket.functions.metadata.FunctionMetadataServiceImpl
import com.kartverket.functions.metadata.dto.CreateFunctionMetadataDTO
import com.kartverket.functions.metadata.dto.FunctionMetadata
import com.kartverket.functions.metadata.dto.UpdateFunctionMetadataDTO
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class FunctionMetadataIntegrationTest {

    @Test
    fun `Create, read, update and delete function metadata`() = testApplication {
        val database = JDBCDatabase.create(testDatabase.getTestdatabaseConfig())
        val microsoftService = object : MockMicrosoftService {}
        val functionMetadataService = FunctionMetadataServiceImpl(database, microsoftService)
        application {
            testModule(
                database,
                authService = AuthServiceImpl(functionMetadataService, microsoftService),
                functionMetadataService = functionMetadataService
            )
        }

        val functionName = "${UUID.randomUUID()}"

        val createFunctionDto = CreateFunctionDto(
            name = functionName, description = "desc", parentId = 1
        )

        var response = client.post("/functions") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    CreateFunctionWithMetadataDto(
                        function = createFunctionDto, metadata = emptyList()
                    )
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val function: Function = Json.decodeFromString(response.bodyAsText())

        // Create meatadata for function
        val createFunctionMetadataDTO = CreateFunctionMetadataDTO(key = "${UUID.randomUUID()}", value = "value")
        response = client.post("/functions/${function.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(createFunctionMetadataDTO))
        }
        assertEquals(HttpStatusCode.NoContent, response.status)

        // Get metadata to verify fields
        val metadata = client.get("/functions/${function.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        val metadataList: List<FunctionMetadata> = Json.decodeFromString(metadata.bodyAsText())

        assertEquals(metadataList.size, 1)
        metadataList.first().let {
            assertEquals(function.id, it.functionId)
            assertEquals(createFunctionMetadataDTO.key, it.key)
            assertEquals(createFunctionMetadataDTO.value, it.value)
        }

        // Update metadata
        val newMetadataValue = "New value"
        val request = UpdateFunctionMetadataDTO(newMetadataValue)

        response = client.patch("/metadata/${metadataList.first().id}") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.NoContent, response.status)

        // Get updated metadata to verify that the value is updated
        val updatedMetadata = client.get("/functions/${function.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        val updatedMetadataList: List<FunctionMetadata> = Json.decodeFromString(updatedMetadata.bodyAsText())
        assertEquals(metadataList.size, 1)
        assertEquals(newMetadataValue, updatedMetadataList.first().value)

        // Delete metadata
        response = client.delete("/metadata/${metadataList.first().id}") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)

        //Try to get deleted metadata to verify that it's deleted
        val deletedMetadata = client.get("/functions/${function.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        Json.decodeFromString<List<FunctionMetadata>>(deletedMetadata.bodyAsText()).let {
            assertTrue(it.isEmpty())
        }
    }

    companion object {

        private lateinit var testDatabase: TestDatabase

        @JvmStatic
        @BeforeAll
        fun setup() {
            testDatabase = TestDatabase()
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            testDatabase.stopTestDatabase()
        }
    }
}