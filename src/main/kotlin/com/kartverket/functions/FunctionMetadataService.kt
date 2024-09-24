package com.kartverket.functions

import com.kartverket.Database
import kotlinx.serialization.Serializable
import java.sql.ResultSet

@Serializable
data class FunctionMetadata(
    val key: String,
    val value: String,
)

object FunctionMetadataService {

    fun getFunctionsByMetadataKeyValuePair(key: String, value: String): List<Function> {
        val functions = mutableListOf<Function>()
        val query = "SELECT f.* " +
                "FROM functions f " +
                "WHERE EXISTS ( " +
                "    SELECT 1 " +
                "    FROM function_metadata fm " +
                "    JOIN function_metadata_keys fmk ON fm.key_id = fmk.id " +
                "    WHERE fm.function_id = f.id " +
                "      AND fmk.key = ? " +
                "      AND fm.value = ?" +
                ");"

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, key)
                statement.setString(2, value)
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    functions.add(resultSet.toFunction())
                }
            }
        }
        return functions
    }

    fun getFunctionMetadataByFunctionId(functionId: Int): List<FunctionMetadata> {
        val metadata = mutableListOf<FunctionMetadata>()
        val query =
            "SELECT fmk.key, fm.value FROM function_metadata fm INNER JOIN function_metadata_keys fmk ON fm.key_id = fmk.id WHERE fm.function_id = ?"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, functionId)
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        metadata.add(resultSet.toFunctionMetadata())
                    }
                }
            }
        }
        return metadata
    }

    fun getFunctionMetadataKeys(search: String?): List<String> {
        val keys = mutableListOf<String>()
        lateinit var query: String
        if (search != null) {
            query = "SELECT key FROM function_metadata_keys WHERE LOWER(key) LIKE ?"
        } else {
            query = "SELECT key FROM function_metadata_keys"
        }

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                if (search != null) {
                    statement.setString(1, search)
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

    fun addMetadataToFunction(functionId: Int, key: String, value: String) {
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
                statement.setString(1, key)
                statement.setString(2, value)
                statement.setInt(3, functionId)
                statement.executeUpdate()
            }
        }
    }


    private fun ResultSet.toFunctionMetadata(): FunctionMetadata {
        return FunctionMetadata(
            key = getString("key"),
            value = getString("value")
        )
    }
}