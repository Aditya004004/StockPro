package com.company.stockpro.feature.settings.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.TeamMember
import com.company.stockpro.core.domain.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamUiState(
    val members: List<TeamMember> = emptyList(),
    val filterActiveOnly: Boolean? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val repository: InventoryRepository,
) : ViewModel() {

    private val filterActiveOnlyFlow = MutableStateFlow<Boolean?>(null)
    private val isLoadingFlow = MutableStateFlow(false)
    private val errorMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TeamUiState> = combine(
        repository.observeTeamMembers(),
        filterActiveOnlyFlow,
        isLoadingFlow,
        errorMessageFlow,
    ) { members, filter, loading, error ->
        val filtered = when (filter) {
            true -> members.filter { it.isActive }
            false -> members.filter { !it.isActive }
            null -> members
        }
        TeamUiState(
            members = filtered,
            filterActiveOnly = filter,
            isLoading = loading,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TeamUiState(),
    )

    fun setFilter(activeOnly: Boolean?) {
        filterActiveOnlyFlow.value = activeOnly
    }

    fun inviteMember(email: String, displayName: String, role: Role, plantId: String = "plant_01") {
        viewModelScope.launch {
            try {
                repository.inviteMember(email, displayName, role, plantId)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                errorMessageFlow.value = e.localizedMessage ?: "Failed to invite member"
            }
        }
    }

    fun updateRole(memberId: String, newRole: Role) {
        viewModelScope.launch {
            try {
                repository.updateMemberRole(memberId, newRole)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                errorMessageFlow.value = e.localizedMessage ?: "Failed to update role"
            }
        }
    }

    fun deactivateMember(memberId: String) {
        viewModelScope.launch {
            try {
                repository.deactivateMember(memberId)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                errorMessageFlow.value = e.localizedMessage ?: "Failed to deactivate member"
            }
        }
    }
}
