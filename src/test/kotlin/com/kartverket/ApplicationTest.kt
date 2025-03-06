import com.kartverket.Database
import com.kartverket.module
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.fail

class ApplicationTest {
    @Test
    fun `Verify that authentication is enabled on non-public endpoints`() = testApplication {
        application {
            // Mock the Database object with stubs to avoid actual DB initialization and migrations during the test
            mockkObject(Database) {
                every { Database.initDatabase() } returns Unit
                every { Database.migrate() } returns Unit
                module()
                routing {
                    val publicEndpointsRegexList = listOf(
                        Regex("^/swagger"),
                        Regex("^/health")
                    )

                    // Get all registered routes and filter out those that match any of the public endpoint regex patterns
                    val nonPublicRoutes = getAllRoutes().filter { route ->
                        publicEndpointsRegexList.none { regex ->
                            regex.containsMatchIn(route.toString())
                        }
                    }

                    assertAll("Authentication in routes", nonPublicRoutes.map { route ->
                        // The `assertionForRoute@` is a label to enable us to return from the function if we find
                        // the Authenticate plugin.
                        assertionForRoute@ {
                            var currRoute: Route? = route
                            // The Authenticate plugin that we are looking for is possibly defined earlier in
                            // the route hierarchy, so we traverse upwards via the parent property.
                            while (currRoute != null) {
                                // Checks if the Authenticate plugin is enabled in the current routes pipeline
                                if (currRoute.items.any { it.name == "Authenticate" }) {
                                    return@assertionForRoute
                                }
                                currRoute = currRoute.parent
                            }

                            fail("$route does not have authentication enabled")
                        }
                    }
                    )
                }
            }
        }
    }

    @Test
    fun `Verify that CORS is enabled`() = testApplication {
        application {
            // Mock the Database object with stubs to avoid actual DB initialization and migrations during the test
            mockkObject(Database) {
                every { Database.initDatabase() } returns Unit
                every { Database.migrate() } returns Unit
                module()
            }
        }

        val failedCors = client.get("/health") {
            header(HttpHeaders.Origin, "https://test1234.com")
        }

        assertEquals(HttpStatusCode.Forbidden, failedCors.status)

        val successCors = client.get("/health") {
            header(HttpHeaders.Origin, "https://test.com")
        }

        assertEquals(HttpStatusCode.OK, successCors.status)
    }
}
