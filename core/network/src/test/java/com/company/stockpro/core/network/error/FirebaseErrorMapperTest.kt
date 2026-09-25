package com.company.stockpro.core.network.error

import com.company.stockpro.core.domain.util.UiText
import com.google.firebase.FirebaseException
import com.google.firebase.functions.FirebaseFunctionsException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Unit tests validating all error mapping branches in [FirebaseErrorMapper]
 * according to the specification table in rules.md §6.1.
 */
class FirebaseErrorMapperTest {

    @Test
    fun `rethrows CancellationException unconditionally per rules section 2_3`() {
        val cancellation = CancellationException("Coroutines cancelled")
        try {
            FirebaseErrorMapper.map(cancellation)
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("Coroutines cancelled", e.message)
        }
    }

    @Test
    fun `maps IOException to offline message`() {
        val result = FirebaseErrorMapper.map(IOException("Socket timeout"))
        assertTrue(result is UiText.Dynamic)
        assertEquals("You're offline. Changes are saved and will sync.", (result as UiText.Dynamic).value)
    }

    @Test
    fun `maps PERMISSION_DENIED functions error`() {
        val ex = createFunctionsException(
            "User is not authorized",
            FirebaseFunctionsException.Code.PERMISSION_DENIED,
        )
        val result = FirebaseErrorMapper.map(ex)
        assertTrue(result is UiText.Dynamic)
        assertEquals("You don't have permission for this.", (result as UiText.Dynamic).value)
    }

    @Test
    fun `maps INSUFFICIENT_STOCK precondition failure`() {
        val ex = createFunctionsException(
            "Insufficient stock. Only 8 pcs available.",
            FirebaseFunctionsException.Code.FAILED_PRECONDITION,
        )
        val result = FirebaseErrorMapper.map(ex)
        assertTrue(result is UiText.Dynamic)
        assertEquals("Insufficient stock. Only 8 pcs available.", (result as UiText.Dynamic).value)
    }

    @Test
    fun `maps missing index or generic failed precondition`() {
        val ex = createFunctionsException(
            "The query requires an index: https://console.firebase.google.com/...",
            FirebaseFunctionsException.Code.FAILED_PRECONDITION,
        )
        val result = FirebaseErrorMapper.map(ex)
        assertTrue(result is UiText.Dynamic)
        assertEquals("Something went wrong loading this.", (result as UiText.Dynamic).value)
    }

    @Test
    fun `maps NOT_FOUND functions error`() {
        val ex = createFunctionsException(
            "Item not found",
            FirebaseFunctionsException.Code.NOT_FOUND,
        )
        val result = FirebaseErrorMapper.map(ex)
        assertTrue(result is UiText.Dynamic)
        assertEquals("This item no longer exists.", (result as UiText.Dynamic).value)
    }

    @Test
    fun `maps UNAUTHENTICATED functions error`() {
        val ex = createFunctionsException(
            "Auth expired",
            FirebaseFunctionsException.Code.UNAUTHENTICATED,
        )
        val result = FirebaseErrorMapper.map(ex)
        assertTrue(result is UiText.Dynamic)
        assertEquals("Session expired. Please sign in again.", (result as UiText.Dynamic).value)
    }

    @Test
    fun `maps ALREADY_EXISTS idempotent replay`() {
        val ex = createFunctionsException(
            "Mutation already posted",
            FirebaseFunctionsException.Code.ALREADY_EXISTS,
        )
        val result = FirebaseErrorMapper.map(ex)
        assertTrue(result is UiText.Dynamic)
        assertEquals("Action already completed.", (result as UiText.Dynamic).value)
    }

    @Test
    fun `maps generic unknown exception`() {
        val ex = IllegalStateException("Unexpected crash")
        val result = FirebaseErrorMapper.map(ex)
        assertTrue(result is UiText.Dynamic)
        assertEquals("Something went wrong. Please try again.", (result as UiText.Dynamic).value)
    }

    private fun createFunctionsException(
        message: String,
        code: FirebaseFunctionsException.Code,
    ): FirebaseFunctionsException {
        val constructor = FirebaseFunctionsException::class.java.getDeclaredConstructor(
            String::class.java,
            FirebaseFunctionsException.Code::class.java,
            Any::class.java,
        )
        constructor.isAccessible = true
        return constructor.newInstance(message, code, null)
    }
}
