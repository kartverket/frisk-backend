package com.kartverket.functions.datadump

import com.kartverket.TestUtils.generateTestToken
import com.kartverket.TestUtils.testModule
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DataDumpRoutesTest {

    @Test
    fun `get dump`() = testApplication {
        application {
            testModule(dataDumpService = object : MockDataDumpService {
                override fun getDataDump(): List<DumpRow> {
                    return emptyList()
                }
            })
        }

        val response = client.get("/dump") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken()}")
        }
        assertEquals(HttpStatusCode.OK, response.status)

    }
}
