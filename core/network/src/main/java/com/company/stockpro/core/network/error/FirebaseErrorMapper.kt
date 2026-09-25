package com.company.stockpro.core.network.error

import android.util.Log
import com.company.stockpro.core.domain.util.UiText
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Maps Firebase and network exceptions across the data/network boundary to [UiText]
 * per rules.md §6.1.
 *
 * Invariant: No raw exception text or HTTP code ever reaches a user.
 */
object FirebaseErrorMapper {

    private const val TAG = "FirebaseErrorMapper"

    fun map(throwable: Throwable): UiText {
        // rules.md §2.3: CancellationException must never be swallowed or mapped as an error
        if (throwable is CancellationException) {
            throw throwable
        }

        return when (throwable) {
            is FirebaseNetworkException, is IOException -> {
                Log.i(TAG, "Network unavailable: ${throwable.message}")
                UiText.Dynamic("You're offline. Changes are saved and will sync.")
            }

            is FirebaseFunctionsException -> mapFunctionsException(throwable)
            is FirebaseFirestoreException -> mapFirestoreException(throwable)

            is FirebaseException -> {
                Log.e(TAG, "Unhandled FirebaseException: ${throwable.message}", throwable)
                UiText.Dynamic("Something went wrong. Please try again.")
            }

            else -> {
                Log.e(TAG, "Unexpected error: ${throwable.message}", throwable)
                UiText.Dynamic("Something went wrong. Please try again.")
            }
        }
    }

    private fun mapFunctionsException(e: FirebaseFunctionsException): UiText {
        return when (e.code) {
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> {
                Log.w(TAG, "PERMISSION_DENIED: ${e.message}")
                UiText.Dynamic("You don't have permission for this.")
            }

            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> {
                // Check if message is about insufficient stock
                val msg = e.message ?: ""
                if (msg.contains("Insufficient stock", ignoreCase = true)) {
                    Log.i(TAG, "INSUFFICIENT_STOCK: $msg")
                    UiText.Dynamic(msg)
                } else {
                    Log.e(TAG, "FAILED_PRECONDITION: $msg", e)
                    UiText.Dynamic("Something went wrong loading this.")
                }
            }

            FirebaseFunctionsException.Code.NOT_FOUND -> {
                Log.i(TAG, "NOT_FOUND: ${e.message}")
                UiText.Dynamic("This item no longer exists.")
            }

            FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                Log.w(TAG, "UNAUTHENTICATED: ${e.message}")
                UiText.Dynamic("Session expired. Please sign in again.")
            }

            FirebaseFunctionsException.Code.UNAVAILABLE -> {
                Log.i(TAG, "UNAVAILABLE: ${e.message}")
                UiText.Dynamic("You're offline. Changes are saved and will sync.")
            }

            FirebaseFunctionsException.Code.ALREADY_EXISTS -> {
                Log.d(TAG, "ALREADY_EXISTS (idempotent replay): ${e.message}")
                UiText.Dynamic("Action already completed.")
            }

            else -> {
                Log.e(TAG, "Functions error [${e.code}]: ${e.message}", e)
                UiText.Dynamic("Something went wrong. Please try again.")
            }
        }
    }

    private fun mapFirestoreException(e: FirebaseFirestoreException): UiText {
        return when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                Log.w(TAG, "Firestore PERMISSION_DENIED: ${e.message}")
                UiText.Dynamic("You don't have permission for this.")
            }

            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> {
                // Missing composite index is a deployment bug
                Log.e(TAG, "CRITICAL: Firestore missing index or failed precondition: ${e.message}", e)
                UiText.Dynamic("Something went wrong loading this.")
            }

            FirebaseFirestoreException.Code.NOT_FOUND -> {
                Log.i(TAG, "Firestore NOT_FOUND: ${e.message}")
                UiText.Dynamic("This item no longer exists.")
            }

            FirebaseFirestoreException.Code.UNAVAILABLE -> {
                Log.i(TAG, "Firestore UNAVAILABLE: ${e.message}")
                UiText.Dynamic("You're offline. Changes are saved and will sync.")
            }

            FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                Log.w(TAG, "Firestore UNAUTHENTICATED: ${e.message}")
                UiText.Dynamic("Session expired. Please sign in again.")
            }

            else -> {
                Log.e(TAG, "Firestore error [${e.code}]: ${e.message}", e)
                UiText.Dynamic("Something went wrong. Please try again.")
            }
        }
    }
}
