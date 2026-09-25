package com.company.stockpro.core.domain.util

sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class StringResource(val id: Int, val args: List<Any> = emptyList()) : UiText
}
