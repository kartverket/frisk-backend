package com.kartverket

import java.sql.Connection

interface MockDatabase : Database {
    override fun getDump(): List<DumpRow> = TODO("Not yet implemented")
    override fun getConnection(): Connection = TODO("Not yet implemented")
}