package com.kartverket.functions.datadump
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.kartverket.Database
import kotlinx.serialization.Serializable
import java.sql.ResultSet
import io.ktor.util.logging.KtorSimpleLogger

@Serializable
data class DumpRow(
    val id: Int,
    val name: String,
    val description: String?,
    val parentId: Int?,
    val path: String,
    val metadata: Map<String, String>
)

interface DataDumpService {
    fun getDataDump() : List<DumpRow>
}

class DataDumpServiceImpl(
    private val database: Database
): DataDumpService {
    private val logger = KtorSimpleLogger("DataDumpService")

    override fun getDataDump(): List<DumpRow> {
        logger.info("Getting datadump of functions and metadata")
        val query ="""
            WITH data_raw AS (
                    SELECT f.id AS function_id,
                           f.name,
                           f.description,
                           f.parent_id,
                           f.path,
                           fmk.key AS metadata_key_name,
                           fm.value AS metadata_value
                    FROM functions f
                             INNER JOIN function_metadata fm ON fm.function_id = f.id
                             INNER JOIN function_metadata_keys fmk ON fmk.id = fm.key_id
                )
                SELECT function_id, name, description, parent_id, path,
                       jsonb_object_agg(metadata_key_name, metadata_value) AS metadata
                FROM data_raw
                GROUP BY function_id, name, description, parent_id, path
        """.trimIndent()

        logger.debug("Preparing database query: $query")
        database.getConnection().use { connection ->
            connection.prepareStatement(query).use { preparedStatement ->
                val resultSet = preparedStatement.executeQuery()
                return buildList {
                    while (resultSet.next()) {
                        add(resultSet.toDumpRow())
                    }
                }.sortedBy { it.id }
            }
        }
    }

}

private fun ResultSet.toDumpRow(): DumpRow {
    return DumpRow(
        id = getInt("function_id"),
        parentId = getInt("parent_id"),
        name = getString("name"),
        description = getString("description"),
        path = getString("path"),
        metadata = parseMetadata(getString("metadata")),
    )
}

private fun parseMetadata(metadataJson: String?): Map<String, String> {
    return if (!metadataJson.isNullOrBlank()) {
            ObjectMapper().readValue(metadataJson, object : TypeReference<Map<String, String>>() {})
    } else {
        emptyMap()
    }
}

