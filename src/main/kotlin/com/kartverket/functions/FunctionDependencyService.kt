package com.kartverket.functions

import com.kartverket.Database
import kotlinx.serialization.Serializable
import java.sql.ResultSet

@Serializable
data class FunctionDependency(val functionId: Int, val dependencyFunctionId: Int)

object FunctionDependencyService {
    fun getFunctionDependencies(id: Int): List<Int> {
        val deps = mutableListOf<Int>()
        val query = "SELECT * FROM function_dependencies WHERE function_id = ?"

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, id)
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    val functionDep = resultSet.toFunctionDependency()
                    deps.add(functionDep.dependencyFunctionId)
                }
            }
        }
        return deps
    }

    fun getFunctionDependents(id: Int): List<Int> {
        val deps = mutableListOf<Int>()
        val query = "SELECT * FROM function_dependencies WHERE dependency_function_id = ?"

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, id)
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    val functionDep = resultSet.toFunctionDependency()
                    deps.add(functionDep.functionId)
                }
            }
        }
        return deps
    }

    fun createFunctionDependency(newDependency: FunctionDependency): FunctionDependency? {
        val query = "INSERT INTO function_dependencies (function_id, dependency_function_id) VALUES (?, ?) RETURNING *"

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, newDependency.functionId)
                statement.setInt(2, newDependency.dependencyFunctionId)
                val resultSet = statement.executeQuery()
                if (!resultSet.next()) {
                    return null
                }
                return resultSet.toFunctionDependency()
            }
        }
    }

    fun deleteFunctionDependency(dependency: FunctionDependency) {
        val query = "DELETE FROM function_dependencies WHERE function_id = ? AND dependency_function_id = ?"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, dependency.functionId)
                statement.setInt(2, dependency.dependencyFunctionId)
                statement.executeUpdate()
            }
        }
    }

    private fun ResultSet.toFunctionDependency(): FunctionDependency {
        return FunctionDependency(
            functionId = getInt("function_id"),
            dependencyFunctionId = getInt("dependency_function_id")
        )
    }
}