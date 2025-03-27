package com.kartverket.functions.datadump

import com.kartverket.Database
import com.kartverket.JDBCDatabase
import com.kartverket.MockMicrosoftService
import com.kartverket.TestDatabase
import com.kartverket.functions.FunctionServiceImpl
import com.kartverket.functions.dto.CreateFunctionDto
import com.kartverket.functions.metadata.FunctionMetadataServiceImpl
import com.kartverket.functions.metadata.dto.CreateFunctionMetadataDTO
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class DataDumpServiceTest {

    @Test
    fun `fetch data dump`() {
        val dataDumpService = DataDumpServiceImpl(database)
        val functionService = FunctionServiceImpl(database)
        val microsoftService = object : MockMicrosoftService {}
        val functionMetadataService = FunctionMetadataServiceImpl(database, microsoftService)

        // Create function
        val functionName = "${UUID.randomUUID()}"
        val createFunctionDto = CreateFunctionDto(
            name = functionName, parentId = 1
        )
        val createdFunction = functionService.createFunction(createFunctionDto)!!
        val fetchedFunction = functionService.getFunction(createdFunction.id)
        assertEquals(createdFunction, fetchedFunction)

        // Create metadata for function
        val createFunctionMetadataDTO1 = CreateFunctionMetadataDTO(key = "beskrivelse", value = "Her er en beskrivelse")
        val createFunctionMetadataDTO2 = CreateFunctionMetadataDTO(key = "kritikalitet", value = "Høy")
        functionMetadataService.addMetadataToFunction(createdFunction.id, createFunctionMetadataDTO1)
        functionMetadataService.addMetadataToFunction(createdFunction.id, createFunctionMetadataDTO2)
        val fetchedMetdata = functionMetadataService.getFunctionMetadataById(1)
        assertEquals(fetchedMetdata?.key, createFunctionMetadataDTO1.key)
        assertEquals(fetchedMetdata?.value, createFunctionMetadataDTO1.value)
        assertEquals(fetchedMetdata?.functionId, createdFunction.id)

        // Fetch data dump
        val dataDump = dataDumpService.getDataDump()
        val expectedMetadata = mapOf("beskrivelse" to "Her er en beskrivelse", "kritikalitet" to "Høy")
        val expectedResponse = listOf(DumpRow(fetchedFunction!!.id, functionName, 1, "1.2", expectedMetadata))
        assertEquals(expectedResponse, dataDump)

    }


    companion object {
        private lateinit var testDatabase: TestDatabase
        private lateinit var database: Database

        @JvmStatic
        @BeforeAll
        fun setup() {
            testDatabase = TestDatabase()
            database = JDBCDatabase.create(testDatabase.getTestdatabaseConfig())
        }

        @JvmStatic
        @AfterAll
        fun stopDatabase() {
            testDatabase.stopTestDatabase()
        }
    }
}
