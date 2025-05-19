package com.kartverket.functions.metadata

import com.kartverket.Database
import com.kartverket.functions.Function
import com.kartverket.functions.metadata.dto.CreateFunctionMetadataDTO
import com.kartverket.functions.metadata.dto.FunctionMetadata
import com.kartverket.functions.metadata.dto.UpdateFunctionMetadataDTO
import com.kartverket.microsoft.MicrosoftService
import com.kartverket.useStatement
import com.microsoft.graph.models.odataerrors.ODataError
import java.sql.ResultSet
import io.ktor.util.logging.KtorSimpleLogger

enum class SpecialMetadataKey(val key: String) {
    TEAM("team")
}

interface FunctionMetadataService {
    fun getFunctionMetadataById(id: Int): FunctionMetadata?
    fun getFunctionMetadata(functionId: Int?, key: String?, value: String?): List<FunctionMetadata>
    fun getFunctionMetadataKeys(search: String? = null): List<String>
    fun addMetadataToFunction(functionId: Int, newMetadata: CreateFunctionMetadataDTO)
    fun updateMetadataValue(id: Int, updatedMetadata: UpdateFunctionMetadataDTO)
    fun deleteMetadata(id: Int)
    fun getIndicators(key: String, value: String?, functionId: Int): List<Function>
}

class FunctionMetadataServiceImpl(
    private val database: Database,
    private val microsoftService: MicrosoftService,
) : FunctionMetadataService {
    private val logger = KtorSimpleLogger("FunctionMetadataService")

    override fun getFunctionMetadataById(id: Int): FunctionMetadata? {
        logger.info("Getting metadata with id: $id")
        val query = """
            SELECT fm.id, fm.function_id, fmk.key, fm.value 
            FROM function_metadata fm 
            INNER JOIN function_metadata_keys fmk ON fm.key_id = fmk.id
            WHERE fm.id = ?
        """.trimIndent()

        database.useStatement(query) { statement ->
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
        return null
    }

    override fun getFunctionMetadata(functionId: Int?, key: String?, value: String?): List<FunctionMetadata> {
        logger.info("Getting metadata for function: $functionId on key: $key and value: $value")
        require(!(functionId == null && key == null)) { "functionId and key cannot both be null" }

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

        database.useStatement(query) { statement ->
            parameters.forEachIndexed { index, param ->
                when (param) {
                    is Int -> statement.setInt(index + 1, param)
                    is String -> statement.setString(index + 1, param)
                    else -> throw IllegalArgumentException("Unsupported parameter type")
                }
            }
            val resultSet = statement.executeQuery()
            return buildList {
                while (resultSet.next()) {
                    add(resultSet.toFunctionMetadata())
                }
            }
        }
    }


    override fun getFunctionMetadataKeys(search: String?): List<String> {
        logger.info("Getting metadatakeys for function with search query: $search")

        val query = if (search != null) {
            "SELECT key FROM function_metadata_keys WHERE LOWER(key) LIKE ?"
        } else {
            "SELECT key FROM function_metadata_keys"
        }

        database.useStatement(query) { statement ->
            if (search != null) {
                statement.setString(1, "%${search.lowercase()}%")
            }
            val resultSet = statement.executeQuery()
            return buildList {
                while (resultSet.next()) {
                    val key = resultSet.getString("key")
                    add(key)
                }
            }
        }
    }

    override fun addMetadataToFunction(functionId: Int, newMetadata: CreateFunctionMetadataDTO) {
        logger.info("Adding metadata: (key: ${newMetadata.key}, value: ${newMetadata.value}) to function: $functionId")
        require(isValueValidForKey(newMetadata.key, newMetadata.value)) {
            "The value ${newMetadata.value} is not a valid for key ${newMetadata.key}"
        }

        val query = """
                INSERT INTO function_metadata_keys (key) 
                VALUES (?) 
                ON CONFLICT (key) DO NOTHING; 
                
                INSERT INTO function_metadata (function_id, key_id, value) 
                VALUES (?, (SELECT id FROM function_metadata_keys WHERE key = ?), ?)
                """.trimIndent()

        database.useStatement(query) { statement ->
            statement.setString(1, newMetadata.key.lowercase())
            statement.setInt(2, functionId)
            statement.setString(3, newMetadata.key.lowercase())
            statement.setString(4, newMetadata.value)
            statement.executeUpdate()
        }
    }

    override fun updateMetadataValue(id: Int, updatedMetadata: UpdateFunctionMetadataDTO) {
        logger.info("Updating metadata (id: $id): value: ${updatedMetadata.value}")
        val query = "UPDATE function_metadata SET value = ? WHERE id = ?"
        database.useStatement(query) { statement ->
            statement.setString(1, updatedMetadata.value)
            statement.setInt(2, id)
            statement.executeUpdate()
        }
    }

    override fun deleteMetadata(id: Int) {
        logger.info("Deleting metadata (id: $id)")
        val query = "DELETE FROM function_metadata WHERE id = ?"
        database.useStatement(query) { statement ->
            statement.setInt(1, id)
            statement.executeUpdate()
        }
    }

    override fun getIndicators(key: String, value: String?, functionId: Int): List<Function> {
        logger.info("Getting indicators (key: $key, value: $value, functionId: $functionId)")
        var query = "WITH fpath AS (SELECT path FROM functions WHERE id = ?)" +
                "SELECT * FROM functions AS f " +
                "INNER JOIN function_metadata AS fm ON f.id = fm.function_id " +
                "INNER JOIN function_metadata_keys AS fmk on fm.key_id = fmk.id " +
                "CROSS JOIN fpath " +
                "WHERE fmk.key = ? AND (f.path <@ fpath.path OR f.path @> fpath.path)"

        if (value != null) {
            query += " AND fm.value = ?"
        }

        database.useStatement(query) { statement ->
            statement.setInt(1, functionId)
            statement.setString(2, key)
            if (value != null) {
                statement.setString(3, value)
            }

            val resultSet = statement.executeQuery()
            return buildList {
                while (resultSet.next()) {
                    add(
                        Function(
                            id = resultSet.getInt("id"),
                            name = resultSet.getString("name"),
                            path = resultSet.getString("path"),
                            parentId = resultSet.getInt("parent_id"),
                            orderIndex = resultSet.getInt("order_index"),
                        )
                    )
                }
            }
        }
    }

    private fun isValueValidForKey(key: String, value: String): Boolean {
        return when (key) {
            SpecialMetadataKey.TEAM.key -> {
                try {
                    val group = microsoftService.getGroup(value)
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
