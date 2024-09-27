import com.kartverket.Database
import com.kartverket.plugins.configureAuth
import com.kartverket.plugins.configureCors
import com.kartverket.plugins.configureRouting
import com.kartverket.plugins.configureSerialization
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            Database.initDatabase()
            Database.migrate()

            configureSerialization()
            configureCors()
            configureAuth()
            configureRouting()

            environment.monitor.subscribe(ApplicationStopped) {
                Database.closePool()
            }
        }
        val response = client.get("/functions")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}