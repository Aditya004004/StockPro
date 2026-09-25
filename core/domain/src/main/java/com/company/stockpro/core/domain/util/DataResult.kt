package com.company.stockpro.core.domain.util

sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Error(val message: UiText, val cause: Throwable? = null) : DataResult<Nothing>
}
