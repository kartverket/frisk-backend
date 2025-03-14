package com.kartverket.functions

import com.kartverket.functions.dto.CreateFunctionDto
import com.kartverket.functions.dto.UpdateFunctionDto

interface MockFunctionService : FunctionService {
    override fun getFunctions(search: String?): List<Function> {
        TODO("Not yet implemented")
    }

    override fun getFunction(id: Int): Function? {
        TODO("Not yet implemented")
    }

    override fun getChildren(id: Int): List<Function> {
        TODO("Not yet implemented")
    }

    override fun createFunction(newFunction: CreateFunctionDto): Function? {
        TODO("Not yet implemented")
    }

    override fun updateFunction(id: Int, updatedFunction: UpdateFunctionDto): Function? {
        TODO("Not yet implemented")
    }

    override fun deleteFunction(id: Int): Boolean {
        TODO("Not yet implemented")
    }

}