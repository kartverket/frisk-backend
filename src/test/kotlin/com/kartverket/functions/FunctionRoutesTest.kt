package com.kartverket.functions

import com.kartverket.TestUtils.generateTestToken
import com.kartverket.TestUtils.testModule
import com.kartverket.functions.metadata.CreateFunctionMetadataDTO
import com.kartverket.functions.metadata.FunctionMetadataService
import com.kartverket.plugins.hasFunctionAccess
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
import org.junit.jupiter.api.Test
import java.util.UUID

class FunctionRoutesTest {

    @Test
    fun `get functions`() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionService) {

            every { FunctionService.getFunctions(any<String>()) } returns emptyList()

            val response = client.get("/functions") {
                header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun testGetFunctionsUnauthorized() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/functions")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testCreateFunctionWithMetadata() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService) {
            mockkObject(FunctionService) {
                val uniqueName = "${UUID.randomUUID()}"
                val metadata = CreateFunctionMetadataDTO(key = "testKey", value = "testValue")

                val request = CreateFunctionWithMetadataDto(
                    function = CreateFunctionDto(
                        name = uniqueName, description = "desc", parentId = 1
                    ), metadata = listOf(metadata)
                )

                every { FunctionService.createFunction(request.function) } returns Function(
                    id = 0,
                    name = uniqueName,
                    description = "desc",
                    parentId = 1,
                    path = "/functions",
                    orderIndex = 0
                )

                every { FunctionMetadataService.addMetadataToFunction(any(), any()) } returns Unit

                val response = client.post("/functions") {
                    header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(request))
                }

                assertEquals(HttpStatusCode.OK, response.status)

                val createdFunction: Function = Json.decodeFromString<Function>(response.bodyAsText())
                assertEquals(uniqueName, createdFunction.name)

                verify { FunctionMetadataService.addMetadataToFunction(createdFunction.id, metadata) }
                confirmVerified(FunctionMetadataService)
            }
        }
    }

    /*    @Test
        fun testCreateFunctionWithoutMetadata() = testApplication {
            application {
                testModule()
            }

            mockkObject(FunctionMetadataService)

            val uniqueName = "${UUID.randomUUID()}"

            val request = CreateFunctionWithMetadataDto(
                function = CreateFunctionDto(
                    name = uniqueName,
                    description = "desc",
                    parentId = 1
                ),
                metadata = emptyList()
            )

            val response = client.post("/functions") {
                header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(request))
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val createdFunction: Function = Json.decodeFromString<Function>(response.bodyAsText())
            assertEquals(uniqueName, createdFunction.name)

            verify(exactly = 0) { FunctionMetadataService.addMetadataToFunction(any(), any()) }
            confirmVerified(FunctionMetadataService)
        }*/

    @Test
    fun testCreateFunctionInvalidInput() = testApplication {
        application {
            testModule()
        }

        val request = CreateFunctionWithMetadataDto(
            function = CreateFunctionDto(
                name = "", description = "desc", parentId = 1
            ), metadata = emptyList()
        )

        val response = client.post("/functions") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testCreateFunctionUnauthorized() = testApplication {
        application {
            testModule()
        }

        val uniqueName = "${UUID.randomUUID()}"

        val request = CreateFunctionWithMetadataDto(
            function = CreateFunctionDto(
                name = uniqueName, description = "desc", parentId = 1
            ), metadata = emptyList()
        )

        val response = client.post("/functions") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testGetFunctionById() = testApplication {
        application {
            testModule()
        }

        val functionId = 1

        val mockedFunction = Function(
            id = functionId,
            name = "Test Function",
            description = "Test Description",
            path = "1.1",
            parentId = 1,
            orderIndex = 1
        )
        mockkObject(FunctionService)
        every { FunctionService.getFunction(functionId) } returns mockedFunction

        val response = client.get("/functions/$functionId") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val fetchedFunction: Function = Json.decodeFromString<Function>(response.bodyAsText())
        assertEquals(functionId, fetchedFunction.id)
        assertEquals("Test Function", fetchedFunction.name)
        assertEquals("Test Description", fetchedFunction.description)
        assertEquals("1.1", fetchedFunction.path)
        assertEquals(1, fetchedFunction.parentId)
        assertEquals(1, fetchedFunction.orderIndex)

        verify { FunctionService.getFunction(functionId) }
        confirmVerified(FunctionService)
    }

    @Test
    fun testGetFunctionByIdUnauthorized() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/functions/${1}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testGetFunctionByIdNotFound() = testApplication {
        application {
            testModule()
        }

        val functionId = 1234567913

        mockkObject(FunctionService)
        every { FunctionService.getFunction(functionId) } returns null

        val response = client.get("/functions/$functionId") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)

        verify { FunctionService.getFunction(functionId) }
        confirmVerified(FunctionService)
    }

    @Test
    fun testGetFunctionByIdBadRequest() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/functions/invalid_id") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    /*    @Test
        fun testUpdateFunctionById() = testApplication {
            application {
                testModule()
            }

            val createdFunction = createFunction(client, 1)

            val updatedFunctionDto = UpdateFunctionDto(
                name = "${UUID.randomUUID()}",
                description = "Updated Description",
                parentId = createdFunction.parentId,
                orderIndex = createdFunction.orderIndex,
                path = createdFunction.path
            )

            val response = client.put("/functions/${createdFunction.id}") {
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
        }*/

    @Test
    fun testUpdateFunctionByIdBadRequest() = testApplication {
        application {
            testModule()
        }

        val response = client.put("/functions/invalid_id") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody("""{"name": "Updated Name", "description": "Updated Description", "parentId": 2}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `Updating non-existent function`() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionMetadataService) {
            mockkObject(FunctionService) {
                every { FunctionMetadataService.getFunctionMetadata(any(), any(), any()) } returns emptyList()
                every { FunctionService.updateFunction(any(), any()) } returns null
                val response = client.put("/functions/1234567913") {
                    header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
                    contentType(ContentType.Application.Json)
                    setBody("""{"name": "Updated Name", "description": "Updated Description", "parentId": 2, "orderIndex": 1, "path": "1.1"}""")
                }

                assertEquals(HttpStatusCode.NotFound, response.status)
            }
        }
    }

    @Test
    fun testUpdateFunctionUnauthorized() = testApplication {
        application {
            testModule()
        }

        val updatedFunctionDto = UpdateFunctionDto(
            name = "${UUID.randomUUID()}",
            description = "Updated Description",
            parentId = 1,
            orderIndex = 1,
            path = ""
        )

        val response = client.put("/functions/1") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(updatedFunctionDto))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testUpdateFunctionBadRequestBody() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionMetadataService) {
            every { FunctionMetadataService.getFunctionMetadata(any(), any(), any()) } returns emptyList()

            val response = client.put("/functions/1") {
                header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
                contentType(ContentType.Application.Json)
                setBody("""{"name": "", "description": "Updated Description", "parentId": 2}""")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun testDeleteFunctionSuccess() = testApplication {
        application {
            testModule()
        }
        mockkObject(FunctionMetadataService) {
            mockkObject(FunctionService) {
                every { FunctionMetadataService.getFunctionMetadata(any(), any(), any()) } returns emptyList()
                every { FunctionService.deleteFunction(any()) } returns true
                val response = client.delete("/functions/1") {
                    header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
                }

                assertEquals(HttpStatusCode.NoContent, response.status)
                verify { FunctionService.deleteFunction(1) }
                confirmVerified(FunctionService)
            }
        }
    }

    @Test
    fun testDeleteFunctionUnauthorized() = testApplication {
        application {
            testModule()
        }

        val response = client.delete("/functions/${1}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testDeleteFunctionForbidden() = testApplication {
        application {
            testModule()
        }

        val functionId = 1

        mockkObject(FunctionService)
        mockkStatic(::hasFunctionAccess)
        every { any<ApplicationCall>().hasFunctionAccess(functionId) } returns false

        val response = client.delete("/functions/$functionId") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        verify(exactly = 0) { FunctionService.deleteFunction(any()) }
    }

    @Test
    fun testDeleteFunctionInvalidId() = testApplication {
        application {
            testModule()
        }

        val response = client.delete("/functions/invalid_id") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }


    /*    @Test
        fun testGetChildrenSuccess() = testApplication {
            application {
                testModule()
            }

            val parentFunction = createFunction(client, 1)
            val childFunction = createFunction(client, parentFunction.id)

            mockkObject(FunctionService)

            val response = client.get("/functions/${parentFunction.id}/children") {
                header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val childrenFunctions: List<Function> = Json.decodeFromString(response.bodyAsText())
            assertEquals(listOf(childFunction), childrenFunctions)

            verify { FunctionService.getChildren(parentFunction.id) }
            confirmVerified(FunctionService)
        }*/

    @Test
    fun testGetChildrenInvalidId() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/functions/invalid_id/children") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testGetChildrenEmptyList() = testApplication {
        application {
            testModule()
        }

        mockkObject(FunctionService) {
            every { FunctionService.getChildren(any()) } returns emptyList()

            val response = client.get("/functions/1/children") {
                header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val childrenFunctions: List<Function> = Json.decodeFromString(response.bodyAsText())
            assertTrue(childrenFunctions.isEmpty())

            verify { FunctionService.getChildren(1) }
            confirmVerified(FunctionService)
        }
    }
}
