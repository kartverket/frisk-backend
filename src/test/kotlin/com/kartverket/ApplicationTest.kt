package com.kartverket

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
    }
    @Test
    fun testHealth() = testApplication {
        application {
            module()
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Up and running!", response.bodyAsText())
    }

    @Test
    fun testAuhorisationRequiredInEndpoints() = testApplication {
        application {
            module()
        }

        val endpoints = listOf("/functions", "metadata", "/microsoft/me/teams", "/dump")
        endpoints.forEach {
            val response = client.get(it)
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }



}
