package com.kartverket.functions

import com.kartverket.Database
import java.sql.ResultSet
import kotlinx.serialization.Serializable

@Serializable
data class Function(val id: Int, val name: String, val parentId: Int, val path: String)

@Serializable
data class CreateFunctionDto(val name: String, val parentId: Int)

@Serializable
data class UpdateFunctionDto(val name: String? = null, val parentId: Int? = null)

object FunctionService {

    fun getAllFunctions(): List<Function> {
        val functions = mutableListOf<Function>()
        val query = "SELECT * FROM functions"

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
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
        val query = "INSERT INTO functions (name, parent_id) VALUES (?, ?) RETURNING *"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, newFunction.name)
                statement.setInt(2, newFunction.parentId)
                val resultSet = statement.executeQuery()
                if (!resultSet.next()) {
                    return null
                }
                return resultSet.toFunction()
            }
        }
    }

    fun updateFunction(id: Int, updatedFields: UpdateFunctionDto): Function? {
        val fields = mutableListOf<String>()
        val query = buildString {
            append("UPDATE functions SET ")
            if (updatedFields.name != null) fields.add("name = ?")
            if (updatedFields.parentId != null) fields.add("parent_id = ?")
            append(fields.joinToString(", "))
            append(" WHERE id = ? RETURNING *")
        }

        if (fields.isEmpty()) {
            // No fields to update, you might want to handle this case
            return null
        }

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                var paramIndex = 1
                if (updatedFields.name != null) {
                    statement.setString(paramIndex++, updatedFields.name)
                }
                if (updatedFields.parentId != null) {
                    statement.setInt(paramIndex++, updatedFields.parentId)
                }
                statement.setInt(paramIndex, id)
                val resultSet = statement.executeQuery()
                if (!resultSet.next()) {
                    return null
                }
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
            parentId = getInt("parent_id"),
            path = getString("path")
        )
    }
}
