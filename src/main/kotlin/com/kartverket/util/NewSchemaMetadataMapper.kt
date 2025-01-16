package com.kartverket.util

import com.kartverket.Database
import com.kartverket.functions.metadata.CreateFunctionMetadataDTO
import com.kartverket.functions.metadata.FunctionMetadata
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.logging.*
import java.sql.ResultSet

class NewSchemaMetadataMapper {

    private val logger = KtorSimpleLogger("Database")

    suspend fun addNewSchemaMetadata() {
        val oldSchemaMetadata = getOldSchemaMetadata()
        logger.info("Har hentet ut gamle skjema metadata")
        oldSchemaMetadata.forEach { m ->
            val contextId = m.value.split(":").first()
            val tableId = fetchTableId(contextId)
            logger.info("Fikk hentet ut tableId")

            if (tableId != null) {
                logger.info("Prøver å legge til ny skjema metadata")
                addMetadataToFunction(
                    m.functionId,
                    CreateFunctionMetadataDTO(key = tableId, value = contextId)
                )
                logger.info("Lagt til ny skjema metadata")
            }
        }
    }

    fun getOldSchemaMetadata(): List<FunctionMetadata> {
        val query =
            "SELECT * FROM function_metadata fm INNER JOIN function_metadata_keys fmk ON fm.key_id = fmk.id WHERE fmk.key = 'rr-skjema'"
        logger.info("Henter gamle skjema metadata")
        return Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.executeQuery().use { resultSet ->
                    generateSequence {
                        if (resultSet.next()) resultSet.toFunctionMetadata() else null
                    }.toList()
                }
            }
        }
    }

    suspend fun fetchTableId(contextId: String): String? {
        val client = HttpClient(CIO)
        return try {
            val baseUrl = when (System.getenv("environment")) {
                "production" -> {
                    if (System.getenv("platform") == "flyio") {
                        "https://regelrett-frontend-1024826672490.europe-north1.run.app/api"
                    } else {
                        val skipEnv = System.getenv("skipEnv")
                        "https://regelrett.atgcp1-${skipEnv}.kartverket-intern.cloud/api"
                    }
                }
                else -> "https://regelrett-frontend-1024826672490.europe-north1.run.app/api"
            }
            logger.info("Kaller på regelrett")
            val response: HttpResponse =
                client.get("$baseUrl/contexts/$contextId/tableId")
            if (response.status == HttpStatusCode.OK) {
                logger.info("Fått status ok fra regelrett")
                response.bodyAsText()
            } else {
                logger.info("Feil ved henting av tableId fra regelrett")
                null
            }
        } catch (e: Exception) {
            logger.info("Feilmelding ble catchet i fetchTableId ${e.message}")
            null
        } finally {
            logger.info("All good i fetchTableId, lukker klienten")
            client.close()
        }
    }

    fun addMetadataToFunction(functionId: Int, newMetadata: CreateFunctionMetadataDTO) {

        val query = "INSERT INTO function_metadata_keys (key) " +
                "VALUES (?) " +
                "ON CONFLICT (key) DO NOTHING; " +
                "INSERT INTO function_metadata (function_id, key_id, value) " +
                "VALUES ( " +
                "    ?, " +
                "    (SELECT id FROM function_metadata_keys WHERE key = ?), " +
                "    ? " +
                ")" +
                "ON CONFLICT ON CONSTRAINT unique_function_key_value DO NOTHING; "

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, newMetadata.key.lowercase())
                statement.setInt(2, functionId)
                statement.setString(3, newMetadata.key.lowercase())
                statement.setString(4, newMetadata.value)
                statement.executeUpdate()
            }
        }
    }

    private fun ResultSet.toFunctionMetadata(): FunctionMetadata {
        return FunctionMetadata(
            id = getInt("id"),
            functionId = getInt("function_id"),
            key = getString("key"),
            value = getString("value")
        )
    }
}
