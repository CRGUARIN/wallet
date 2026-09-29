package com.crguarin.wallet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.domain.model.UserPreferences
import com.crguarin.wallet.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(
        val onboardingCompleted: Boolean,
        val preferences: UserPreferences,
    ) : MainUiState
}

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        val uiState: StateFlow<MainUiState> =
            preferencesRepository
                .observeUserPreferences()
                .map { prefs ->
                    MainUiState.Ready(
                        onboardingCompleted = prefs.isOnboardingCompleted,
                        preferences = prefs,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = MainUiState.Loading,
                )
    }
