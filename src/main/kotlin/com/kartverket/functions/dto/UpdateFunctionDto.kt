package com.kartverket.functions.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateFunctionDto(
    val name: String,
    val parentId: Int?,
    val path: String,
    val orderIndex: Int,
)
