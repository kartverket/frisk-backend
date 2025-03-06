package com.kartverket.functions.metadata

import com.kartverket.TestUtils.addMetadata
import com.kartverket.TestUtils.createFunction
import com.kartverket.TestUtils.generateTestToken
import com.kartverket.TestUtils.testModule
import com.kartverket.functions.Function
import com.kartverket.plugins.hasFunctionAccess
import com.kartverket.plugins.hasMetadataAccess
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

class FunctionMetadataRoutesTest {

    @Test
    fun testGetFunctionMetadata() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionMetadataService) {
            val metadataKey = "${UUID.randomUUID()}"
            val metadataValue = "value"
            every { FunctionMetadataService.getFunctionMetadata(any(), any(), any()) } returns listOf(
                FunctionMetadata(
                    id = 2,
                    functionId = 1,
                    key = metadataKey,
                    value = metadataValue
                )
            )

            val response = client.get("/functions/1/metadata") {
                header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            verify { FunctionMetadataService.getFunctionMetadata(1, null, null) }

            val metadataList: List<FunctionMetadata> = Json.decodeFromString(response.bodyAsText())
            assertEquals(1, metadataList.size)
            assertEquals(1, metadataList[0].functionId)
            assertEquals(metadataKey, metadataList[0].key)
            assertEquals(metadataValue, metadataList[0].value)
        }
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
    fun testAddMetadataUnauthorized() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService) {
            every { FunctionMetadataService.getFunctionMetadata(any(), any(), any()) } returns emptyList()

            val request = CreateFunctionMetadataDTO(key = "${UUID.randomUUID()}", value = "value")

            val response = client.post("/functions/1/metadata") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(request))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            verify(exactly = 0) { FunctionMetadataService.addMetadataToFunction(any(), any()) }
        }
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

        mockkObject(FunctionMetadataService) {
            mockkStatic(::hasFunctionAccess) {
                every { any<ApplicationCall>().hasFunctionAccess(1) } returns false

                val request = CreateFunctionMetadataDTO(key = "${UUID.randomUUID()}", value = "value")
                val response = client.post("/functions/1/metadata") {
                    header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(request))
                }

                assertEquals(HttpStatusCode.Forbidden, response.status)
                verify(exactly = 0) { FunctionMetadataService.addMetadataToFunction(any(), any()) }
            }
        }
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

        mockkObject(FunctionMetadataService) {
            mockkStatic(::hasMetadataAccess) {
                every { any<ApplicationCall>().hasMetadataAccess(any()) } returns false

                val response = client.patch("/metadata/1") {
                    header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(UpdateFunctionMetadataDTO("new value")))
                }

                assertEquals(HttpStatusCode.Forbidden, response.status)
                verify(exactly = 0) { FunctionMetadataService.updateMetadataValue(any(), any()) }
            }
        }
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