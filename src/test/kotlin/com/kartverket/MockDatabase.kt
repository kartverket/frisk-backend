package com.kartverket

import java.sql.Connection

interface MockDatabase : Database {
    override fun getConnection(): Connection = TODO("Not yet implemented")
}
