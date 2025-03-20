package com.kartverket.functions.metadata

import com.kartverket.functions.Function
import com.kartverket.functions.metadata.dto.CreateFunctionMetadataDTO
import com.kartverket.functions.metadata.dto.FunctionMetadata
import com.kartverket.functions.metadata.dto.UpdateFunctionMetadataDTO

interface MockFunctionMetadataService : FunctionMetadataService {
    override fun getFunctionMetadataById(id: Int): FunctionMetadata? {
        TODO("Not yet implemented")
    }

    override fun getFunctionMetadata(functionId: Int?, key: String?, value: String?): List<FunctionMetadata> {
        TODO("Not yet implemented")
    }

    override fun getFunctionMetadataKeys(search: String?): List<String> {
        TODO("Not yet implemented")
    }

    override fun addMetadataToFunction(functionId: Int, newMetadata: CreateFunctionMetadataDTO) {
        TODO("Not yet implemented")
    }

    override fun updateMetadataValue(id: Int, updatedMetadata: UpdateFunctionMetadataDTO) {
        TODO("Not yet implemented")
    }

    override fun deleteMetadata(id: Int) {
        TODO("Not yet implemented")
    }

    override fun getIndicators(key: String, value: String?, functionId: Int): List<Function> {
        TODO("Not yet implemented")
    }
}