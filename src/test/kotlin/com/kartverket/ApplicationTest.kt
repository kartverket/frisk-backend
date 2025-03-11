import com.kartverket.*
import com.kartverket.configuration.AppConfig
import com.kartverket.configuration.DatabaseConfig
import com.kartverket.configuration.FunctionHistoryCleanupConfig
import com.kartverket.configureAPILayer
import com.kartverket.functions.FunctionServiceImpl
import com.kartverket.functions.metadata.FunctionMetadataServiceImpl
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.fail

class ApplicationTest {
    private val exampleConfig = AppConfig(FunctionHistoryCleanupConfig(1, 1), emptyList(), DatabaseConfig("", "", ""))

    @Test
    fun `Verify that authentication is enabled on non-public endpoints`() = testApplication {
        application {
            val mockDatabase = object : MockDatabase {}
            configureAPILayer(exampleConfig, mockDatabase, object : MockAuthService {}, FunctionServiceImpl(mockDatabase), FunctionMetadataServiceImpl(mockDatabase))
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
                    assertionForRoute@{
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

    @Test
    fun `Verify that CORS is enabled`() = testApplication {
        application {
            val mockDatabase = object : MockDatabase {}
            configureAPILayer(
                exampleConfig.copy(
                    allowedCORSHosts = listOf("test.com")
                ),
                mockDatabase,
                object : MockAuthService {},
                FunctionServiceImpl(mockDatabase),
                FunctionMetadataServiceImpl(mockDatabase)
            )
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
