package com.kartverket.functions

import com.kartverket.MockAuthService
import com.kartverket.TestUtils.generateTestToken
import com.kartverket.TestUtils.testModule
import com.kartverket.functions.dto.CreateFunctionDto
import com.kartverket.functions.dto.CreateFunctionWithMetadataDto
import com.kartverket.functions.dto.UpdateFunctionDto
import com.kartverket.functions.metadata.dto.CreateFunctionMetadataDTO
import com.kartverket.functions.metadata.MockFunctionMetadataService
import com.kartverket.auth.UserId
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID

class FunctionRoutesTest {

    @Test
    fun `get functions`() = testApplication {
        application {
            testModule(functionService = object : MockFunctionService {
                override fun getFunctions(search: String?): List<Function> {
                    return emptyList()
                }
            })
        }

        val response = client.get("/functions") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        assertEquals(HttpStatusCode.OK, response.status)

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
        var functionMetadataWasAdded = false
        application {
            testModule(
                functionService = object : MockFunctionService {
                    override fun createFunction(newFunction: CreateFunctionDto): Function? {
                        return Function(
                            id = 0,
                            name = newFunction.name,
                            parentId = 1,
                            path = "/functions",
                            orderIndex = 0
                        )
                    }
                },
                functionMetadataService = object : MockFunctionMetadataService {
                    override fun addMetadataToFunction(functionId: Int, newMetadata: CreateFunctionMetadataDTO) {
                        assertEquals(0, functionId)
                        functionMetadataWasAdded = true
                    }
                }
            )
        }
        val uniqueName = "${UUID.randomUUID()}"
        val metadata = CreateFunctionMetadataDTO(key = "testKey", value = "testValue")

        val request = CreateFunctionWithMetadataDto(
            function = CreateFunctionDto(
                name = uniqueName, parentId = 1
            ), metadata = listOf(metadata)
        )

        val response = client.post("/functions") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val createdFunction: Function = Json.decodeFromString<Function>(response.bodyAsText())
        assertEquals(uniqueName, createdFunction.name)
        assertTrue(functionMetadataWasAdded)
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
                name = "", parentId = 1
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
                name = uniqueName, parentId = 1
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
        val functionId = 1
        val mockedFunction = Function(
            id = functionId,
            name = "Test Function",
            path = "1.1",
            parentId = 1,
            orderIndex = 1
        )

        application {
            testModule(
                functionService = object : MockFunctionService {
                    override fun getFunction(id: Int): Function? {
                        return mockedFunction
                    }
                }
            )
        }

        val response = client.get("/functions/$functionId") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val fetchedFunction: Function = Json.decodeFromString<Function>(response.bodyAsText())
        assertEquals(functionId, fetchedFunction.id)
        assertEquals("Test Function", fetchedFunction.name)
        assertEquals("1.1", fetchedFunction.path)
        assertEquals(1, fetchedFunction.parentId)
        assertEquals(1, fetchedFunction.orderIndex)
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
            testModule(functionService = object : MockFunctionService {
                override fun getFunction(id: Int): Function? = null
            })
        }

        val response = client.get("/functions/1234567913") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)

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
            testModule(
                authService = object : MockAuthService {
                    override fun hasFunctionAccess(userId: UserId, functionId: Int): Boolean = true
                },
                functionService = object : MockFunctionService {
                    override fun updateFunction(id: Int, updatedFunction: UpdateFunctionDto): Function? {
                        return null
                    }
                }
            )
        }


        val response = client.put("/functions/1234567913") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody("""{"name": "Updated Name", "parentId": 2, "orderIndex": 1, "path": "1.1"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)

    }

    @Test
    fun testUpdateFunctionUnauthorized() = testApplication {
        application {
            testModule()
        }

        val updatedFunctionDto = UpdateFunctionDto(
            name = "${UUID.randomUUID()}",
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
    @Disabled("Can't see any validation of request body, so this does not test what it says")
    fun testUpdateFunctionBadRequestBody() = testApplication {
        application {
            testModule()
        }

        val response = client.put("/functions/1") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
            contentType(ContentType.Application.Json)
            setBody("""{"name": "", "description": "Updated Description", "parentId": 2}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testDeleteFunctionSuccess() = testApplication {
        var deletedId: Int? = null
        application {
            testModule(
                authService = object : MockAuthService {
                    override fun hasFunctionAccess(userId: UserId, functionId: Int): Boolean = true
                },
                functionService = object : MockFunctionService {
                    override fun deleteFunction(id: Int): Boolean {
                        deletedId = id
                        return true
                    }
                })
        }

        val response = client.delete("/functions/1") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(1, deletedId)
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
        var deletedWasCalled = false
        application {
            testModule(
                authService = object : MockAuthService {
                    override fun hasFunctionAccess(userId: UserId, functionId: Int): Boolean = false
                    override fun hasSuperUserAccess(userId: UserId): Boolean = false

                },
                functionService = object : MockFunctionService {
                    override fun deleteFunction(id: Int): Boolean {
                        deletedWasCalled = true
                        return true
                    }
                })
        }

        val functionId = 1

        val response = client.delete("/functions/$functionId") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertFalse(deletedWasCalled)
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


    @Test
    fun testGetChildrenSuccess() = testApplication {
        val children = listOf(
            Function(
                id = 2,
                parentId = 1,
                name = "${UUID.randomUUID()}",
                orderIndex = 1,
                path = "/function/children",
            )
        )
        application {
            testModule(functionService = object : MockFunctionService {
                override fun getChildren(id: Int): List<Function> {
                    return children
                }
            })
        }

        val response = client.get("/functions/1234/children") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val childrenFromApi: List<Function> = Json.decodeFromString(response.bodyAsText())
        assertEquals(children, childrenFromApi)
    }

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
        var parentId: Int? = null
        application {
            testModule(functionService = object : MockFunctionService {
                override fun getChildren(id: Int): List<Function> {
                    parentId = id
                    return emptyList()
                }
            })
        }

        val response = client.get("/functions/1/children") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val childrenFunctions: List<Function> = Json.decodeFromString(response.bodyAsText())
        assertTrue(childrenFunctions.isEmpty())

        assertEquals(1, parentId)
    }
}
