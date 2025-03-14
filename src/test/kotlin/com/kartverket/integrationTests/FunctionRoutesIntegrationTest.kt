package com.kartverket.integrationTests

import com.kartverket.JDBCDatabase
import com.kartverket.TestDatabase
import com.kartverket.TestUtils.generateTestToken
import com.kartverket.TestUtils.testModule
import com.kartverket.auth.AuthServiceImpl
import com.kartverket.functions.dto.CreateFunctionDto
import com.kartverket.functions.dto.CreateFunctionWithMetadataDto
import com.kartverket.functions.Function
import com.kartverket.functions.dto.UpdateFunctionDto
import com.kartverket.functions.metadata.FunctionMetadataServiceImpl
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class FunctionRoutesIntegrationTest {

    @Test
    fun `Create, read, update and delete function`() = testApplication {
        val database = JDBCDatabase.create(testDatabase.getTestdatabaseConfig())
        val functionMetadataService = FunctionMetadataServiceImpl(database)
        application {
            testModule(database, authService = AuthServiceImpl(functionMetadataService), functionMetadataService = functionMetadataService)
        }

        val functionName = "${UUID.randomUUID()}"

        val function = CreateFunctionDto(
            name = functionName,
            description = "desc",
            parentId = 1
        )

        val request = CreateFunctionWithMetadataDto(
            function = function,
            metadata = emptyList()
        )

        var response = client.post("/functions") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val createdFunction: Function = Json.decodeFromString(response.bodyAsText())

        assertEquals(functionName, createdFunction.name)
        assertEquals(function.description, createdFunction.description)
        assertEquals(function.parentId, createdFunction.parentId)

        response = client.get("/functions/${createdFunction.id}") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val readFunction = Json.decodeFromString<Function>(response.bodyAsText())
        assertEquals(functionName, readFunction.name)
        assertEquals(function.description, readFunction.description)
        assertEquals(function.parentId, readFunction.parentId)

        val updatedFunctionDto = UpdateFunctionDto(
            name = "${UUID.randomUUID()}",
            description = "Updated Description",
            parentId = createdFunction.parentId,
            orderIndex = createdFunction.orderIndex,
            path = createdFunction.path
        )

        response = client.put("/functions/${createdFunction.id}") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(updatedFunctionDto))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val updatedFunction: Function = Json.decodeFromString(response.bodyAsText())
        assertEquals(updatedFunctionDto.name, updatedFunction.name)
        assertEquals(updatedFunctionDto.description, updatedFunction.description)
        assertEquals(updatedFunctionDto.parentId, updatedFunction.parentId)
        assertEquals(updatedFunctionDto.orderIndex, updatedFunction.orderIndex)
        assertEquals(updatedFunctionDto.path, updatedFunction.path)

        response = client.delete("/functions/${createdFunction.id}") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.NoContent, response.status)

        response = client.get("/functions/${createdFunction.id}") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
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