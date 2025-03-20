package com.kartverket.functions.metadata.dto

import kotlinx.serialization.Serializable

@Serializable
data class FunctionMetadata(
    val id: Int,
    val functionId: Int,
    val key: String,
    val value: String,
)