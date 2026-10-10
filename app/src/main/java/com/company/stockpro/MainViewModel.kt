package com.company.stockpro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.stockpro.core.domain.model.AppNotification
import com.company.stockpro.core.domain.model.FailedOutboxEntry
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.ToolCustody
import com.company.stockpro.core.domain.model.UserProfile
import com.company.stockpro.core.domain.model.UserSettings
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.usecase.PostMovementUseCase
import com.company.stockpro.core.domain.model.ThresholdAudit
import com.company.stockpro.core.domain.util.DataResult
import com.company.stockpro.core.domain.util.UiText
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppAuthState {
    data object Loading : AppAuthState
    data class Authenticated(val userId: String, val email: String) : AppAuthState
    data object Unauthenticated : AppAuthState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val postMovementUseCase: PostMovementUseCase,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _authState = MutableStateFlow<AppAuthState>(AppAuthState.Loading)
    val authState: StateFlow<AppAuthState> = _authState.asStateFlow()

    init {
        checkAuthSession()
        try {
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    _authState.value = AppAuthState.Authenticated(user.uid, user.email ?: "")
                } else {
                    _authState.value = AppAuthState.Unauthenticated
                }
            }
        } catch (_: Exception) {}
    }

    fun checkAuthSession() {
        val user = try { auth.currentUser } catch (_: Exception) { null }
        if (user != null) {
            _authState.value = AppAuthState.Authenticated(user.uid, user.email ?: "")
        } else {
            _authState.value = AppAuthState.Unauthenticated
        }
    }

    fun onAuthenticated() {
        val user = try { auth.currentUser } catch (_: Exception) { null }
        if (user != null) {
            _authState.value = AppAuthState.Authenticated(user.uid, user.email ?: "")
        }
    }

    val userSettings: StateFlow<UserSettings> = repository
        .observeUserSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings(
                profile = UserProfile(
                    uid = "",
                    displayName = "User",
                    email = "",
                    role = Role.INVENTORY_MANAGER,
                    plantId = "plant_01",
                    plantName = "Pune Plant 1",
                ),
            ),
        )

    val failedEntries: StateFlow<List<FailedOutboxEntry>> = repository
        .observeFailedEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val notifications: StateFlow<List<AppNotification>> = repository
        .observeNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun observeItem(itemId: String): Flow<Item?> = repository.observeItem(itemId)

    fun observeLedger(itemId: String): Flow<List<StockMovement>> = repository.observeLedger(itemId)

    fun observeToolCustody(itemId: String): Flow<ToolCustody?> = repository.observeToolCustody(itemId)

    fun postMovement(
        movement: StockMovement,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                when (val result = postMovementUseCase(movement)) {
                    is DataResult.Success -> onSuccess()
                    is DataResult.Error -> {
                        val msg = (result.message as? UiText.Dynamic)?.value ?: result.message.toString()
                        onError(msg)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onError(e.localizedMessage ?: "Failed to post movement")
            }
        }
    }

    fun createItem(
        item: Item,
        onSuccess: (Item) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                when (val result = repository.createItem(item)) {
                    is DataResult.Success -> onSuccess(result.data)
                    is DataResult.Error -> {
                        val msg = (result.message as? UiText.Dynamic)?.value ?: result.message.toString()
                        onError(msg)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onError(e.localizedMessage ?: "Failed to create item")
            }
        }
    }

    fun retryFailedOutbox(id: Long) {
        viewModelScope.launch {
            try {
                repository.retryFailedOutbox(id)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun discardFailedOutbox(id: Long) {
        viewModelScope.launch {
            try {
                repository.discardFailedOutbox(id)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun resolveBarcode(
        barcode: String,
        onFound: (Item) -> Unit,
        onNotFound: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val result = repository.getItemByCode(barcode)
                if (result is DataResult.Success) {
                    onFound(result.data)
                } else {
                    onNotFound()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onNotFound()
            }
        }
    }

    fun resolveNotificationEntity(
        codeOrId: String,
        onFound: (Item) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val res = repository.getItem(codeOrId)
                if (res is DataResult.Success) {
                    onFound(res.data)
                } else {
                    val codeRes = repository.getItemByCode(codeOrId)
                    if (codeRes is DataResult.Success) {
                        onFound(codeRes.data)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                repository.markAllNotificationsAsRead()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            try {
                repository.clearNotifications()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun issueTool(itemId: String, operator: String, notes: String?) {
        viewModelScope.launch {
            try {
                repository.issueTool(itemId, operator, notes)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun returnTool(itemId: String, notes: String?) {
        viewModelScope.launch {
            try {
                repository.returnTool(itemId, notes)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun logCalibration(itemId: String, cal: Long, next: Long, notes: String?) {
        viewModelScope.launch {
            try {
                repository.logCalibration(itemId, cal, next, notes)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun updateItemThreshold(
        itemId: String,
        newThresholdMilli: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val operator = userSettings.value.profile.displayName
                when (val result = repository.updateItemThreshold(itemId, newThresholdMilli, operator)) {
                    is DataResult.Success -> onSuccess()
                    is DataResult.Error -> {
                        val msg = (result.message as? UiText.Dynamic)?.value ?: result.message.toString()
                        onError(msg)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onError(e.localizedMessage ?: "Failed to update threshold")
            }
        }
    }

    fun observeThresholdAudits(itemId: String): Flow<List<ThresholdAudit>> =
        repository.observeThresholdAudits(itemId)

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.logout()
                auth.signOut()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            _authState.value = AppAuthState.Unauthenticated
            onComplete()
        }
    }
}
