package com.kartverket.functions.metadata.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateFunctionMetadataDTO(
    val value: String,
)