package com.company.stockpro.core.domain.model

/**
 * Domain entity representing a member of the plant team.
 * architecture.md §4.1 / prd.md §4.1 (A6)
 */
data class TeamMember(
    val id: String,
    val displayName: String,
    val email: String,
    val role: Role,
    val plantId: String,
    val isActive: Boolean = true,
    val lastActiveAt: Long = 0L,
) {
    val initials: String
        get() {
            val parts = displayName.trim().split("\\s+".toRegex())
            return when {
                parts.isEmpty() || parts[0].isEmpty() -> "U"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "${parts[0].first()}${parts[1].first()}".uppercase()
            }
        }
}
