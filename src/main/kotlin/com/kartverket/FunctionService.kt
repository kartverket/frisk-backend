package com.kartverket

import java.sql.ResultSet

data class Function(val id: Int, val name: String)

object FunctionService {

    fun getAllFunctions(): List<Function> {
        val users = mutableListOf<Function>()
        val query = "SELECT * FROM functions"

        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    users.add(resultSet.toFunction())
                }
            }
        }
        return users
    }

    fun addFunction(name: String, email: String): Boolean {
        val query = "INSERT INTO functions (name, email) VALUES (?, ?)"
        Database.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, name)
                statement.setString(2, email)
                return statement.executeUpdate() > 0
            }
        }
    }

    private fun ResultSet.toFunction(): Function {
        return Function(
            id = getInt("id"),
            name = getString("name"),
        )
    }
}
