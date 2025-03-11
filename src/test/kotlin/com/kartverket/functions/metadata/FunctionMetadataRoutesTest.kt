package com.kartverket.functions.metadata

import com.kartverket.MockAuthService
import com.kartverket.TestUtils.generateTestToken
import com.kartverket.TestUtils.testModule
import com.kartverket.functions.metadata.dto.CreateFunctionMetadataDTO
import com.kartverket.functions.metadata.dto.FunctionMetadata
import com.kartverket.functions.metadata.dto.UpdateFunctionMetadataDTO
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class FunctionMetadataRoutesTest {

    @Test
    fun testGetFunctionMetadata() = testApplication {
        val metadataKey = "${UUID.randomUUID()}"
        val metadataValue = "value"
        application {
            testModule(
                functionMetadataService = object : MockFunctionMetadataService {
                    override fun getFunctionMetadata(
                        functionId: Int?,
                        key: String?,
                        value: String?
                    ): List<FunctionMetadata> = listOf(
                        FunctionMetadata(
                            id = 2,
                            functionId = 1,
                            key = metadataKey,
                            value = metadataValue
                        )
                    )
                }
            )
        }

        val response = client.get("/functions/1/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val metadataList: List<FunctionMetadata> = Json.decodeFromString(response.bodyAsText())
        assertEquals(1, metadataList.size)
        assertEquals(1, metadataList[0].functionId)
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
    fun testAddMetadataUnauthorized() = testApplication {
        application {
            testModule()
        }
        val request = CreateFunctionMetadataDTO(key = "${UUID.randomUUID()}", value = "value")

        val response = client.post("/functions/1/metadata") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testAddMetadataInvalidId() = testApplication {
        application {
            testModule()
        }

        val request = CreateFunctionMetadataDTO(key = "${UUID.randomUUID()}", value = "value")
        val response = client.post("/functions/invalid_id/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testAddMetadataForbidden() = testApplication {
        application {
            testModule(
                authService = object : MockAuthService {
                    override fun hasFunctionAccess(call: ApplicationCall, functionId: Int): Boolean = false
                }
            )
        }

        val request = CreateFunctionMetadataDTO(key = "${UUID.randomUUID()}", value = "value")
        val response = client.post("/functions/1/metadata") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testGetMetadataUnauthorized() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/metadata?key=${UUID.randomUUID()}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testGetMetadataIndicatorsUnauthorized() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/metadata/indicator?key=${UUID.randomUUID()}&functionId=${UUID.randomUUID()}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testGetMetadataKeysUnauthorized() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/metadata/keys")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testUpdateMetadataUnauthorized() = testApplication {
        application {
            testModule()
        }

        val response = client.patch("/metadata/1") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateFunctionMetadataDTO("new value")))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testUpdateMetadataInvalidId() = testApplication {
        application {
            testModule()
        }
        val response = client.patch("/metadata/invalid_Id") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateFunctionMetadataDTO("new value")))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testUpdateMetadataForbidden() = testApplication {
        application {
            testModule(
                authService = object : MockAuthService {
                    override fun hasMetadataAccess(call: ApplicationCall, metadataId: Int) = false
                }
            )
        }

        val response = client.patch("/metadata/1") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateFunctionMetadataDTO("new value")))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testDeleteMetadataUnauthorized() = testApplication {
        application {
            testModule()
        }

        val response = client.delete("/metadata/1")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testDeleteMetadataInvalidId() = testApplication {
        application {
            testModule()
        }
        val response = client.delete("/metadata/invalid_Id") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testDeleteMetadataForbidden() = testApplication {
        application {
            testModule(
                authService = object : MockAuthService {
                    override fun hasMetadataAccess(call: ApplicationCall, metadataId: Int): Boolean = false
                }
            )
        }

        val response = client.delete("/metadata/1") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}