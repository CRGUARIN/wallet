package com.crguarin.wallet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.domain.model.UserPreferences
import com.crguarin.wallet.domain.repository.PreferencesRepository
import com.crguarin.wallet.domain.usecase.notifications.OnAppStartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
        private val onAppStartUseCase: OnAppStartUseCase,
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

        // Runs once per ViewModel lifetime (survives recomposition and rotation):
        // processes due recurring payments and ensures the daily work is scheduled.
        private var appStartHandled = false

        init {
            viewModelScope.launch {
                preferencesRepository.observeUserPreferences().collect { prefs ->
                    if (prefs.isOnboardingCompleted && !appStartHandled) {
                        appStartHandled = true
                        onAppStartUseCase()
                    }
                }
            }
        }
    }
