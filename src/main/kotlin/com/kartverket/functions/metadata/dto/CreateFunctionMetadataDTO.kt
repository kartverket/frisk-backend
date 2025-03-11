package com.kartverket.functions.metadata.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateFunctionMetadataDTO(
    val key: String,
    val value: String,
)