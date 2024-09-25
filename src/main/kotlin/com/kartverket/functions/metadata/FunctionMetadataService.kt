package com.kartverket.functions.metadata

import com.kartverket.Database
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

object FunctionMetadataService {

    fun getFunctionMetadata(functionId: Int?, key: String?, value: String?): List<FunctionMetadata> {
        if (functionId == null && key == null) {
            throw IllegalArgumentException("functionId and key can not be null")
        }
        val metadata = mutableListOf<FunctionMetadata>()

        var query = "SELECT fm.id, fm.function_id, fmk.key, fm.value FROM function_metadata fm INNER JOIN function_metadata_keys fmk ON fm.key_id = fmk.id"
        query += if (functionId != null && key != null) {
            " WHERE fm.function_id = ? AND fmk.key = ?"
        } else if (functionId != null) {
            " WHERE fm.function_id = ?"
        } else {
            " WHERE fmk.key = ?"
        }

        if (value != null) {
            query += " AND fm.value = ?"
        }

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                var index = 1
                if (functionId != null) {
                    statement.setInt(index++, functionId)
                }
                if (key != null) {
                    statement.setString(index++, key.lowercase())
                }
                if (value != null) {
                    statement.setString(index, value)
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


    private fun ResultSet.toFunctionMetadata(): FunctionMetadata {
        return FunctionMetadata(
            id = getInt("id"),
            functionId = getInt("function_id"),
            key = getString("key"),
            value = getString("value")
        )
    }
}