package com.kartverket.functions

import com.kartverket.Database
import com.kartverket.functions.dto.CreateFunctionDto
import com.kartverket.functions.dto.UpdateFunctionDto
import com.kartverket.useStatement
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.Serializable
import java.sql.ResultSet
import java.sql.Types

@Serializable
data class Function(
    val id: Int,
    val name: String,
    val parentId: Int?,
    val path: String,
    val orderIndex: Int
)

interface FunctionService {
    fun getFunctions(search: String? = null): List<Function>
    fun getFunction(id: Int): Function?
    fun getChildren(id: Int): List<Function>
    fun createFunction(newFunction: CreateFunctionDto): Function?
    fun updateFunction(
        id: Int,
        updatedFunction: UpdateFunctionDto,
    ): Function?

    fun deleteFunction(id: Int): Boolean
}

class FunctionServiceImpl(
    private val database: Database
) : FunctionService {
    private val logger = KtorSimpleLogger("FunctionService")

    override fun getFunctions(search: String?): List<Function> {
        logger.info("Getting functions with search query: $search")
        val query: String = if (search != null) {
            "SELECT * FROM functions WHERE LOWER(name) LIKE ?"
        } else {
            "SELECT * FROM functions"
        }

        logger.debug("Preparing database query: $query")
        database.useStatement(query) { statement ->
            if (search != null) {
                statement.setString(1, "%${search.lowercase()}%")
            }
            val resultSet = statement.executeQuery()
            return buildList {
                while (resultSet.next()) {
                    add(resultSet.toFunction())
                }
            }
        }
    }

    override fun getFunction(id: Int): Function? {
        logger.info("Getting function with id: $id")
        val query = "SELECT * FROM functions where id = ?"
        logger.debug("Preparing database query: $query")
        database.useStatement(query) { statement ->
            statement.setInt(1, id)
            val resultSet = statement.executeQuery()
            if (!resultSet.next()) {
                return null
            }
            return resultSet.toFunction()
        }
    }

    override fun getChildren(id: Int): List<Function> {
        logger.info("Getting childeren with: $id")
        val query = "SELECT * FROM functions WHERE parent_id = ? ORDER BY order_index"
        logger.debug("Preparing database query: $query")
        database.useStatement(query) { statement ->
            statement.setInt(1, id)
            val resultSet = statement.executeQuery()
            return buildList {
                while (resultSet.next()) {
                    add(resultSet.toFunction())
                }
            }
        }
    }

    override fun createFunction(newFunction: CreateFunctionDto): Function? {
        logger.info("Creating function with: ${newFunction.name}")
        val query = "INSERT INTO functions (name, parent_id) VALUES (?, ?) RETURNING *"
        logger.debug("Preparing database query: $query")
        database.useStatement(query) { statement ->
            statement.setString(1, newFunction.name)
            statement.setInt(2, newFunction.parentId)
            val resultSet = statement.executeQuery()
            if (!resultSet.next()) {
                return null
            }
            return resultSet.toFunction()
        }
    }

    override fun updateFunction(
        id: Int,
        updatedFunction: UpdateFunctionDto,
    ): Function? {
        logger.info("Updating function with id: $id")
        var result: Function? = null

        val query = "SELECT * FROM update_function(?, ?, ?, ?);"
        logger.debug("Preparing database query: $query")
        database.useStatement(query) { statement ->
            statement.setInt(1, id)
            statement.setInt(2, updatedFunction.orderIndex)
            statement.setString(3, updatedFunction.name)
            if (updatedFunction.parentId != null) {
                statement.setInt(4, updatedFunction.parentId)
            } else {
                statement.setNull(4, Types.INTEGER)
            }

            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                result = resultSet.toFunction()
            }
        }
        return result
    }

    override fun deleteFunction(id: Int): Boolean {
        logger.info("Deleting function with id: $id")
        val query = "DELETE FROM functions WHERE id = ?"
        logger.debug("Preparing database query: $query")
        database.useStatement(query) { statement ->
            statement.setInt(1, id)
            return statement.executeUpdate() > 0
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
            parentId,
            path = getString("path"),
            orderIndex = getInt("order_index")
        )
    }
}
