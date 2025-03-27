package com.kartverket.functions.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateFunctionDto(
    val name: String,
    val parentId: Int,
)
