package com.kartverket.functions.metadata

import com.kartverket.Database
import com.kartverket.microsoft.MicrosoftService
import com.microsoft.graph.models.odataerrors.ODataError
import kotlinx.serialization.Serializable
import java.sql.ResultSet

@Serializable
data class FunctionMetadata(
    val id: Int,
    val functionId: Int,
    val key: String,
    val value: String,
)

@Serializable
data class CreateFunctionMetadataDTO(
    val key: String,
    val value: String,
)

@Serializable
data class UpdateFunctionMetadataDTO(
    val value: String,
)

enum class SpecialMetadataKey(val key: String) {
    TEAM("team")
}

object FunctionMetadataService {

    fun getFunctionMetadataById(id: Int): FunctionMetadata? {
        val query = """
            SELECT fm.id, fm.function_id, fmk.key, fm.value 
            FROM function_metadata fm 
            INNER JOIN function_metadata_keys fmk ON fm.key_id = fmk.id
            WHERE fm.id = ?
        """.trimIndent()

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, id)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    return FunctionMetadata(
                        id = resultSet.getInt("id"),
                        functionId = resultSet.getInt("function_id"),
                        key = resultSet.getString("key"),
                        value = resultSet.getString("value"),
                    )
                }
            }
        }
        return null
    }

    fun getFunctionMetadata(functionId: Int?, key: String?, value: String?): List<FunctionMetadata> {
        require(!(functionId == null && key == null)) { "functionId and key cannot both be null" }

        val metadata = mutableListOf<FunctionMetadata>()

        val baseQuery = """
            SELECT fm.id, fm.function_id, fmk.key, fm.value 
            FROM function_metadata fm 
            INNER JOIN function_metadata_keys fmk ON fm.key_id = fmk.id
        """.trimIndent()

        val conditions = mutableListOf<String>()
        val parameters = mutableListOf<Any?>()

        if (functionId != null) {
            conditions.add("fm.function_id = ?")
            parameters.add(functionId)
        }
        if (key != null) {
            conditions.add("fmk.key = ?")
            parameters.add(key.lowercase())
        }
        if (value != null) {
            conditions.add("fm.value = ?")
            parameters.add(value)
        }

        val whereClause = if (conditions.isNotEmpty()) " WHERE ${conditions.joinToString(" AND ")}" else ""
        val query = baseQuery + whereClause

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                parameters.forEachIndexed { index, param ->
                    when (param) {
                        is Int -> statement.setInt(index + 1, param)
                        is String -> statement.setString(index + 1, param)
                        else -> throw IllegalArgumentException("Unsupported parameter type")
                    }
                }
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    metadata.add(resultSet.toFunctionMetadata())
                }
            }
        }
        return metadata
    }


    fun getFunctionMetadataKeys(search: String? = null): List<String> {
        val keys = mutableListOf<String>()
        val query = if (search != null) {
            "SELECT key FROM function_metadata_keys WHERE LOWER(key) LIKE ?"
        } else {
            "SELECT key FROM function_metadata_keys"
        }

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                if (search != null) {
                    statement.setString(1, "%${search.lowercase()}%")
                }
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    val key = resultSet.getString("key")
                    keys.add(key)
                }
            }
        }
        return keys
    }

    fun addMetadataToFunction(functionId: Int, newMetadata: CreateFunctionMetadataDTO) {
        require(isValueValidForKey(newMetadata.key, newMetadata.value)) {
            "The value ${newMetadata.value} is not a valid for key ${newMetadata.key}"
        }

        val query = "INSERT INTO function_metadata_keys (key) " +
                "VALUES (?) " +
                "ON CONFLICT (key) DO NOTHING; " +
                "INSERT INTO function_metadata (function_id, key_id, value) " +
                "VALUES ( " +
                "    ?, " +
                "    (SELECT id FROM function_metadata_keys WHERE key = ?), " +
                "    ? " +
                ")"

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

    fun updateMetadataValue(id: Int, updatedMetadata: UpdateFunctionMetadataDTO) {
        val query = "UPDATE function_metadata SET value = ? WHERE id = ?"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, updatedMetadata.value)
                statement.setInt(2, id)
                statement.executeUpdate()
            }
        }
    }

    fun deleteMetadata(id: Int) {
        val query = "DELETE FROM function_metadata WHERE id = ?"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, id)
                statement.executeUpdate()
            }
        }
    }

    private fun isValueValidForKey(key: String, value: String): Boolean {
        return when (key) {
            SpecialMetadataKey.TEAM.key -> {
                try {
                    val group = MicrosoftService.getGroup(value)
                    group.id == value
                } catch (error: ODataError) {
                    false
                }
            }
            else -> true
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