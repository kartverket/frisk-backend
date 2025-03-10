package com.kartverket.functions.dto

import com.kartverket.functions.metadata.CreateFunctionMetadataDTO
import kotlinx.serialization.Serializable

@Serializable
data class CreateFunctionWithMetadataDto(
    val function: CreateFunctionDto,
    val metadata: List<CreateFunctionMetadataDTO>
)