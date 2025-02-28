package com.kartverket.functions.metadata

import com.kartverket.TestUtils
import com.kartverket.TestUtils.addMetadata
import com.kartverket.TestUtils.createFunction
import com.kartverket.TestUtils.generateTestToken
import com.kartverket.TestUtils.postgresContainer
import com.kartverket.TestUtils.testModule
import com.kartverket.functions.Function
import com.kartverket.plugins.hasFunctionAccess
import com.kartverket.plugins.hasMetadataAccess
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FunctionMetadataRoutesTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            postgresContainer.start()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            TestUtils.stopTestDatabase()
        }
    }

    @Test
    fun testGetFunctionMetadata() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionMetadataService)

        val createdFunction = createFunction(client, 1)
        val metadataKey = "${UUID.randomUUID()}"
        val metadataValue = "value"
        addMetadata(client, createdFunction.id, metadataKey, metadataValue)

        val response = client.get("/functions/${createdFunction.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        verify { FunctionMetadataService.getFunctionMetadata(createdFunction.id, null, null) }

        val metadataList: List<FunctionMetadata> = Json.decodeFromString(response.bodyAsText())
        assertEquals(1, metadataList.size)
        assertEquals(createdFunction.id, metadataList[0].functionId)
        assertEquals(metadataKey, metadataList[0].key)
        assertEquals(metadataValue, metadataList[0].value)
    }

    @Test
    fun testGetFunctionMetadataUnauthorized() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/functions/${UUID.randomUUID()}/metadata")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testGetFunctionMetadataInvalidId() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/functions/invalid_id/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testAddMetadata() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionMetadataService)

        val createdFunction = createFunction(client, 1)

        val request = CreateFunctionMetadataDTO(key = "${UUID.randomUUID()}", value = "value")

        val response = client.post("/functions/${createdFunction.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify { FunctionMetadataService.addMetadataToFunction(createdFunction.id, request) }
    }

    @Test
    fun testAddMetadataUnauthorized() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)

        val createdFunction = createFunction(client, 1)
        val request = CreateFunctionMetadataDTO(key = "${UUID.randomUUID()}", value = "value")

        val response = client.post("/functions/${createdFunction.id}/metadata") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        verify(exactly = 0) { FunctionMetadataService.addMetadataToFunction(any(), any()) }
    }

    @Test
    fun testAddMetadataInvalidId() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)

        val request = CreateFunctionMetadataDTO(key = "${UUID.randomUUID()}", value = "value")
        val response = client.post("/functions/invalid_id/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { FunctionMetadataService.addMetadataToFunction(any(), any()) }
    }

    @Test
    fun testAddMetadataForbidden() = testApplication {
        application {
            testModule()
        }

        val createdFunction = createFunction(client, 1)

        mockkObject(FunctionMetadataService)
        mockkStatic(::hasFunctionAccess)
        every { any<ApplicationCall>().hasFunctionAccess(createdFunction.id) } returns false

        val request = CreateFunctionMetadataDTO(key = "${UUID.randomUUID()}", value = "value")
        val response = client.post("/functions/${createdFunction.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        verify(exactly = 0) { FunctionMetadataService.addMetadataToFunction(any(), any()) }
    }

    @Test
    fun testGetMetadata() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)

        val createdFunction = createFunction(client, 1)
        val metadataKey = "${UUID.randomUUID()}"
        val metadataValue = "value"
        addMetadata(client, createdFunction.id, metadataKey, metadataValue)

        val response = client.get("/metadata?key=$metadataKey&value=$metadataValue&functionId=${createdFunction.id}") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify { FunctionMetadataService.getFunctionMetadata(createdFunction.id, metadataKey, metadataValue) }

        val metadataList: List<FunctionMetadata> = Json.decodeFromString(response.bodyAsText())
        assertEquals(1, metadataList.size)
        assertEquals(createdFunction.id, metadataList[0].functionId)
        assertEquals(metadataKey, metadataList[0].key)
        assertEquals(metadataValue, metadataList[0].value)
    }

    @Test
    fun testGetMetadataUnauthorized() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)

        val response = client.get("/metadata?key=${UUID.randomUUID()}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        verify(exactly = 0) { FunctionMetadataService.getFunctionMetadata(any(), any(), any()) }
    }

    @Test
    fun testGetMetadataIndicators() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)

        val function1 = createFunction(client, 1)
        val function2 = createFunction(client, function1.id)
        val metadataKey = "${UUID.randomUUID()}"
        val metadataValue = "value"

        addMetadata(client, function1.id, metadataKey, metadataValue)
        addMetadata(client, function2.id, metadataKey, metadataValue)

        val response =
            client.get("/metadata/indicator?functionId=${function1.id}&key=$metadataKey&value=$metadataValue") {
                header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            }

        assertEquals(HttpStatusCode.OK, response.status)
        verify { FunctionMetadataService.getIndicators(metadataKey, metadataValue, function1.id) }
        val functionList: List<Function> = Json.decodeFromString(response.bodyAsText())
        assertEquals(2, functionList.size)
        assertTrue(functionList.contains(function1) && functionList.contains(function2))
    }

    @Test
    fun testGetMetadataIndicatorsUnauthorized() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)

        val response = client.get("/metadata/indicator?key=${UUID.randomUUID()}&functionId=${UUID.randomUUID()}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        verify(exactly = 0) { FunctionMetadataService.getIndicators(any(), any(), any()) }
    }

    @Test
    fun testGetMetadataKeys() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)

        val function = createFunction(client, 1)
        val metadataKey = "${UUID.randomUUID()}"
        addMetadata(client, function.id, metadataKey, "value")

        val response = client.get("/metadata/keys?search=$metadataKey") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify { FunctionMetadataService.getFunctionMetadataKeys(metadataKey) }
        val metadataKeys: List<String> = Json.decodeFromString(response.bodyAsText())
        assertEquals(1, metadataKeys.size)
        assertEquals(metadataKey, metadataKeys[0])
    }

    @Test
    fun testGetMetadataKeysUnauthorized() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)

        val response = client.get("/metadata/keys")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        verify(exactly = 0) { FunctionMetadataService.getFunctionMetadataKeys(any()) }
    }

    @Test
    fun testUpdateMetadata() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)
        mockkStatic(::hasMetadataAccess)
        every { any<ApplicationCall>().hasMetadataAccess(any()) } returns true

        //create function and add metadata
        val function = createFunction(client, 1)
        addMetadata(client, function.id, "${UUID.randomUUID()}", "value")

        //get added metadata to obtain the metadataId
        val metadata = client.get("/functions/${function.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        val metadataList: List<FunctionMetadata> = Json.decodeFromString(metadata.bodyAsText())

        //Update metadata
        val newMetadataValue = "New value"
        val request = UpdateFunctionMetadataDTO(newMetadataValue)

        val response = client.patch("/metadata/${metadataList[0].id}") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify { FunctionMetadataService.updateMetadataValue(metadataList[0].id, request) }

        //get updated metadata to verify that the value is updated
        val updatedMetadata = client.get("/functions/${function.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        val updatedMetadataList: List<FunctionMetadata> = Json.decodeFromString(updatedMetadata.bodyAsText())
        assertEquals(newMetadataValue, updatedMetadataList[0].value)
    }

    @Test
    fun testUpdateMetadataUnauthorized() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionMetadataService)
        val response = client.patch("/metadata/1") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateFunctionMetadataDTO("new value")))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        verify(exactly = 0) { FunctionMetadataService.updateMetadataValue(any(), any()) }
    }

    @Test
    fun testUpdateMetadataInvalidId() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)
        val response = client.patch("/metadata/invalid_Id") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateFunctionMetadataDTO("new value")))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { FunctionMetadataService.updateMetadataValue(any(), any()) }
    }

    @Test
    fun testUpdateMetadataForbidden() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionMetadataService)
        mockkStatic(::hasMetadataAccess)
        every { any<ApplicationCall>().hasMetadataAccess(any()) } returns false

        val response = client.patch("/metadata/1") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateFunctionMetadataDTO("new value")))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        verify(exactly = 0) { FunctionMetadataService.updateMetadataValue(any(), any()) }
    }

    @Test
    fun testDeleteMetadata() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)
        mockkStatic(::hasMetadataAccess)
        every { any<ApplicationCall>().hasMetadataAccess(any()) } returns true

        //create function and add metadata
        val function = createFunction(client, 1)
        addMetadata(client, function.id, "${UUID.randomUUID()}", "value")

        //get added metadata to obtain the metadataId
        val metadata = client.get("/functions/${function.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        val metadataList: List<FunctionMetadata> = Json.decodeFromString(metadata.bodyAsText())

        //Delete metadata
        val response = client.delete("/metadata/${metadataList[0].id}") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify { FunctionMetadataService.deleteMetadata(metadataList[0].id) }

        //Try to get deleted metadata to verify that it's deleted
        val deletedMetadata = client.get("/functions/${function.id}/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        val updatedMetadataList: List<FunctionMetadata> = Json.decodeFromString(deletedMetadata.bodyAsText())
        assertTrue(updatedMetadataList.isEmpty())
    }

    @Test
    fun testDeleteMetadataUnauthorized() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionMetadataService)
        val response = client.delete("/metadata/1")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        verify(exactly = 0) { FunctionMetadataService.deleteMetadata(any()) }
    }

    @Test
    fun testDeleteMetadataInvalidId() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService)
        val response = client.delete("/metadata/invalid_Id") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { FunctionMetadataService.deleteMetadata(any()) }
    }

    @Test
    fun testDeleteMetadataForbidden() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionMetadataService)
        mockkStatic(::hasMetadataAccess)
        every { any<ApplicationCall>().hasMetadataAccess(any()) } returns false

        val response = client.delete("/metadata/1") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        verify(exactly = 0) { FunctionMetadataService.deleteMetadata(any()) }
    }
}