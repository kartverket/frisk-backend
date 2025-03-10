package com.kartverket.functions

import com.kartverket.Database
import com.kartverket.functions.metadata.CreateFunctionMetadataDTO
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.Serializable
import java.sql.ResultSet
import java.sql.Types

@Serializable
data class Function(
    val id: Int,
    val name: String,
    val description: String?,
    val parentId: Int?,
    val path: String,
    val orderIndex: Int
)

@Serializable
data class CreateFunctionDto(
    val name: String,
    val description: String? = null,
    val parentId: Int,
)

@Serializable
data class CreateFunctionWithMetadataDto(
    val function: CreateFunctionDto,
    val metadata: List<CreateFunctionMetadataDTO>
)


@Serializable
data class UpdateFunctionDto(
    val name: String,
    val description: String? = null,
    val parentId: Int?,
    val path: String,
    val orderIndex: Int,
)

object FunctionService {
    lateinit var database: Database
    val logger = KtorSimpleLogger("FunctionService")

    fun getFunctions(search: String? = null): List<Function> {
        logger.info("Getting functions with search query: $search")
        val functions = mutableListOf<Function>()
        lateinit var query: String
        if (search != null) {
            query = "SELECT * FROM functions WHERE LOWER(name) LIKE ?"
        } else {
            query = "SELECT * FROM functions"
        }

        database.getConnection().use { connection ->
            logger.debug("Preparing database query: $query")
            connection.prepareStatement(query).use { statement ->
                if (search != null) {
                    statement.setString(1, "%${search.lowercase()}%")
                }
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    functions.add(resultSet.toFunction())
                }
            }
        }
        return functions
    }

    fun getFunction(id: Int): Function? {
        logger.info("Getting function with id: $id")
        val query = "SELECT * FROM functions where id = ?"
        logger.debug("Preparing database query: $query")
        database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, id)
                val resultSet = statement.executeQuery()
                if (!resultSet.next()) {
                    return null
                }
                return resultSet.toFunction()
            }
        }
    }

    fun getChildren(id: Int): List<Function> {
        logger.info("Getting childeren with: $id")
        val functions = mutableListOf<Function>()
        val query = "SELECT * FROM functions WHERE parent_id = ? ORDER BY order_index"
        logger.debug("Preparing database query: $query")
        database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, id)
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    functions.add(resultSet.toFunction())
                }
            }
        }
        return functions
    }

    fun createFunction(newFunction: CreateFunctionDto): Function? {
        logger.info("Creating function with: ${newFunction.name}")
        val query = "INSERT INTO functions (name, description, parent_id) VALUES (?, ?, ?) RETURNING *"
        logger.debug("Preparing database query: $query")
        database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, newFunction.name)
                statement.setString(2, newFunction.description)
                statement.setInt(3, newFunction.parentId)
                val resultSet = statement.executeQuery()
                if (!resultSet.next()) {
                    return null
                }
                return resultSet.toFunction()
            }
        }
    }

    fun updateFunction(
        id: Int,
        updatedFunction: UpdateFunctionDto,
    ): Function? {
        logger.info("Updating function with id: $id")
        var result: Function? = null

        val query = "SELECT * FROM update_function(?, ?, ?, ?, ?);"
        logger.debug("Preparing database query: $query")
        database.getConnection().use { connection ->

            connection.prepareStatement(query).use { statement ->
                var i = 1

                statement.setInt(i++, id)
                statement.setInt(i++, updatedFunction.orderIndex)
                statement.setString(i++, updatedFunction.name)
                statement.setString(i++, updatedFunction.description)
                if (updatedFunction.parentId != null) {
                    statement.setInt(i, updatedFunction.parentId)
                } else {
                    statement.setNull(i, Types.INTEGER)
                }

                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    result = resultSet.toFunction()
                }
            }
        }
        return result
    }

    fun deleteFunction(id: Int): Boolean {
        logger.info("Deleting function with id: $id")
        val query = "DELETE FROM functions WHERE id = ?"
        logger.debug("Preparing database query: $query")
        database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, id)
                return statement.executeUpdate() > 0
            }
        }
    }

    private fun ResultSet.toFunction(): Function {
        var parentId: Int? = getInt("parent_id")
        if (wasNull()) {
            parentId = null
        }
        return Function(
            id = getInt("id"),
            name = getString("name"),
            description = getString("description"),
            parentId,
            path = getString("path"),
            orderIndex = getInt("order_index")
        )
    }
}
