package com.company.stockpro.core.domain.model

/**
 * Domain model representing an industrial manufacturing plant or distribution facility.
 */
data class Plant(
    val id: String,
    val code: String,
    val name: String,
    val location: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
