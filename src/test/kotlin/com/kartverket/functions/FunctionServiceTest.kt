package com.kartverket.functions

import com.kartverket.Database
import com.kartverket.JDBCDatabase
import com.kartverket.TestDatabase
import com.kartverket.functions.dto.CreateFunctionDto
import com.kartverket.functions.dto.UpdateFunctionDto
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertContains

class FunctionServiceTest {

    @Test
    fun `create and read function`() {

        val functionService = FunctionServiceImpl(database)

        val created = functionService.createFunction(CreateFunctionDto(name = "test", description = "test", parentId = 1))!!

        val fetched = functionService.getFunction(created.id)

        assertEquals(created, fetched)
    }

    @Test
    fun `fetch children for function`() {
        val functionService = FunctionServiceImpl(database)

        val created = functionService.createFunction(CreateFunctionDto(name = "test", description = "test", parentId = 1))!!
        val (child1, child2, child3) = createThreeChildren(functionService, created)

        assertEquals(created.id, child1.parentId)
        assertEquals(created.id, child2.parentId)
        assertEquals(created.id, child3.parentId)

        val children = functionService.getChildren(created.id)

        assertEquals(3, children.size)
        assertContains(children, child1)
        assertContains(children, child2)
        assertContains(children, child3)
    }

    @Test
    fun `update where first child leaves parent`() {
        val functionService = FunctionServiceImpl(database)

        val created = functionService.createFunction(CreateFunctionDto(name = "test", description = "test", parentId = 1))!!
        var (child1, child2, child3) = createThreeChildren(functionService, created)

        assertEquals(0, child1.orderIndex)
        assertEquals(1, child2.orderIndex)
        assertEquals(2, child3.orderIndex)

        functionService.updateFunction(child1.id, child1.toUpdateDto().copy(parentId = 1))

        child1 = functionService.getFunction(child1.id)!!
        child2 = functionService.getFunction(child2.id)!!
        child3 = functionService.getFunction(child3.id)!!

        assertEquals(0, child1.orderIndex)
        assertEquals(0, child2.orderIndex)
        assertEquals(1, child3.orderIndex)
    }

    @Test
    fun `update where middle child leaves parent`() {
        val functionService = FunctionServiceImpl(database)

        val created = functionService.createFunction(CreateFunctionDto(name = "test", description = "test", parentId = 1))!!
        var (child1, child2, child3) = createThreeChildren(functionService, created)

        assertEquals(0, child1.orderIndex)
        assertEquals(1, child2.orderIndex)
        assertEquals(2, child3.orderIndex)

        functionService.updateFunction(child2.id, child2.toUpdateDto().copy(parentId = 1))

        child1 = functionService.getFunction(child1.id)!!
        child2 = functionService.getFunction(child2.id)!!
        child3 = functionService.getFunction(child3.id)!!

        assertEquals(0, child1.orderIndex)
        assertEquals(1, child2.orderIndex)
        assertEquals(1, child3.orderIndex)
    }

    @Test
    fun `update where last child leaves parent`() {
        val functionService = FunctionServiceImpl(database)

        val created = functionService.createFunction(CreateFunctionDto(name = "test", description = "test", parentId = 1))!!
        val randomfunc = functionService.createFunction(CreateFunctionDto(name = "test4", description = "test", parentId = 1))
        var (child1, child2, child3) = createThreeChildren(functionService, created)

        assertEquals(0, child1.orderIndex)
        assertEquals(1, child2.orderIndex)
        assertEquals(2, child3.orderIndex)

        functionService.updateFunction(child3.id, child3.toUpdateDto().copy(parentId = 1))

        child1 = functionService.getFunction(child1.id)!!
        child2 = functionService.getFunction(child2.id)!!
        child3 = functionService.getFunction(child3.id)!!

        assertEquals(0, child1.orderIndex)
        assertEquals(1, child2.orderIndex)
        assertEquals(2, child3.orderIndex)

        // Nå har randomfunc og child3 samme orderindex = er det ok?
    }

    @Test
    fun `move children from end to start of order`() {
        val functionService = FunctionServiceImpl(database)

        val created = functionService.createFunction(CreateFunctionDto(name = "test", description = "test", parentId = 1))!!
        var (child1, child2, child3) = createThreeChildren(functionService, created)

        assertEquals(0, child1.orderIndex)
        assertEquals(1, child2.orderIndex)
        assertEquals(2, child3.orderIndex)

        functionService.updateFunction(child3.id, child3.toUpdateDto().copy(orderIndex = 0))

        child1 = functionService.getFunction(child1.id)!!
        child2 = functionService.getFunction(child2.id)!!
        child3 = functionService.getFunction(child3.id)!!

        assertEquals(1, child1.orderIndex)
        assertEquals(2, child2.orderIndex)
        assertEquals(0, child3.orderIndex)
    }

    @Test
    fun `move children from start to end of order`() {
        val functionService = FunctionServiceImpl(database)

        val created = functionService.createFunction(CreateFunctionDto(name = "test", description = "test", parentId = 1))!!
        var (child1, child2, child3) = createThreeChildren(functionService, created)

        assertEquals(0, child1.orderIndex)
        assertEquals(1, child2.orderIndex)
        assertEquals(2, child3.orderIndex)

        functionService.updateFunction(child1.id, child1.toUpdateDto().copy(orderIndex = 5))

        child1 = functionService.getFunction(child1.id)!!
        child2 = functionService.getFunction(child2.id)!!
        child3 = functionService.getFunction(child3.id)!!

        assertEquals(5, child1.orderIndex)
        assertEquals(0, child2.orderIndex)
        assertEquals(1, child3.orderIndex)
    }

    @Test
    fun `move children from start to middle of order`() {
        val functionService = FunctionServiceImpl(database)

        val created = functionService.createFunction(CreateFunctionDto(name = "test", description = "test", parentId = 1))!!
        var (child1, child2, child3) = createThreeChildren(functionService, created)

        assertEquals(0, child1.orderIndex)
        assertEquals(1, child2.orderIndex)
        assertEquals(2, child3.orderIndex)

        functionService.updateFunction(child1.id, child1.toUpdateDto().copy(orderIndex = 1))

        child1 = functionService.getFunction(child1.id)!!
        child2 = functionService.getFunction(child2.id)!!
        child3 = functionService.getFunction(child3.id)!!

        assertEquals(1, child1.orderIndex)
        assertEquals(0, child2.orderIndex)
        assertEquals(2, child3.orderIndex)
    }

    private fun createThreeChildren(
        functionService: FunctionServiceImpl,
        created: Function
    ): Triple<Function, Function, Function> {
        val child1 = functionService.createFunction(
            CreateFunctionDto(
                name = "test1",
                description = "test",
                parentId = created.id
            )
        )!!
        val child2 = functionService.createFunction(
            CreateFunctionDto(
                name = "test2",
                description = "test",
                parentId = created.id
            )
        )!!
        val child3 = functionService.createFunction(
            CreateFunctionDto(
                name = "test3",
                description = "test",
                parentId = created.id
            )
        )!!
        return Triple(child1, child2, child3)
    }

    @AfterEach
    fun cleanup() {
        database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("DELETE FROM functions WHERE id > 1")
            }
        }
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

fun Function.toUpdateDto() = UpdateFunctionDto(
    name = this.name,
    description = this.description,
    parentId = this.parentId,
    path = this.path,
    orderIndex = this.orderIndex,
)