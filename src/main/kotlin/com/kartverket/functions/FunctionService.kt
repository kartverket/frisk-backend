package com.kartverket.functions

import com.kartverket.Database
import java.sql.ResultSet
import kotlinx.serialization.Serializable
import java.sql.Types

@Serializable
data class Function(val id: Int, val name: String, val description: String?, val parentId: Int?, val path: String)

@Serializable
data class CreateFunctionDto(val name: String, val description: String? = null, val parentId: Int)

@Serializable
data class UpdateFunctionDto(val name: String, val description: String? = null, val parentId: Int?)

object FunctionService {

    fun getFunctions(search: String? = null): List<Function> {
        val functions = mutableListOf<Function>()
        lateinit var query: String
        if (search != null) {
            query = "SELECT * FROM functions WHERE LOWER(name) LIKE ?"
        } else {
            query = "SELECT * FROM functions"
        }


        Database.getConnection().use { connection ->
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
        val query = "SELECT * FROM functions where id = ?"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use {statement ->
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
        val functions = mutableListOf<Function>()
        val query = "SELECT * FROM functions WHERE parent_id = ?"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use {statement ->
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
        val query = "INSERT INTO functions (name, description, parent_id) VALUES (?, ?, ?) RETURNING *"
        Database.getConnection().use { connection ->
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

    fun updateFunction(id: Int, updatedFunction: UpdateFunctionDto): Function? {
        val query = "UPDATE functions SET name = ?, description = ?, parent_id = ? WHERE id = ? RETURNING *"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, updatedFunction.name)
                statement.setString(2, updatedFunction.description)
                if (updatedFunction.parentId != null) {
                    statement.setInt(3, updatedFunction.parentId)
                } else {
                    statement.setNull(3, Types.INTEGER)
                }
                statement.setInt(4, id)
                val resultSet = statement.executeQuery()
                if (!resultSet.next()) return null
                return resultSet.toFunction()
            }
        }
    }

    fun deleteFunction(id: Int): Boolean {
        val query = "DELETE FROM functions WHERE id = ?"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, id)
                return statement.executeUpdate() > 0
            }
        }
    }

    private fun ResultSet.toFunction(): Function {
        return Function(
            id = getInt("id"),
            name = getString("name"),
            description = getString("description"),
            parentId = getInt("parent_id"),
            path = getString("path")
        )
    }
}
