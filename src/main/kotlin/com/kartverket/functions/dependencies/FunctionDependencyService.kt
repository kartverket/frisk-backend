package com.kartverket.functions.dependencies

import com.kartverket.Database
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.Serializable
import java.sql.ResultSet

@Serializable
data class FunctionDependency(
    val functionId: Int,
    val dependencyFunctionId: Int,
)

object FunctionDependencyService {
    val logger = KtorSimpleLogger("FunctionDependencyService")

    fun getFunctionDependencies(id: Int): List<Int> {
        logger.info("Getting function dependencies with id: $id")
        val deps = mutableListOf<Int>()
        val query = "SELECT * FROM function_dependencies WHERE function_id = ?"
        logger.debug("Preparing database query: $query")
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
        logger.info("Getting function dependents with id: $id")
        val deps = mutableListOf<Int>()
        val query = "SELECT * FROM function_dependencies WHERE dependency_function_id = ?"
        logger.debug("Preparing database query: $query")
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
        logger.info("Creating function dependency btween: ${newDependency.functionId} and ${newDependency.dependencyFunctionId}")
        val query = "INSERT INTO function_dependencies (function_id, dependency_function_id) VALUES (?, ?) RETURNING *"
        logger.debug("Preparing database query: $query")
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
        logger.info("Deleting function dependency with id: ${dependency.functionId}")
        val query = "DELETE FROM function_dependencies WHERE function_id = ? AND dependency_function_id = ?"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, dependency.functionId)
                statement.setInt(2, dependency.dependencyFunctionId)
                statement.executeUpdate()
            }
        }
    }

    private fun ResultSet.toFunctionDependency(): FunctionDependency =
        FunctionDependency(
            functionId = getInt("function_id"),
            dependencyFunctionId = getInt("dependency_function_id"),
        )
}
