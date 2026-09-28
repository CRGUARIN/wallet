package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.model.UserPreferences
import com.crguarin.wallet.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakePreferencesRepository(
    initialPreferences: UserPreferences = UserPreferences(),
) : PreferencesRepository {
    private val preferencesFlow = MutableStateFlow(initialPreferences)

    override fun observeUserPreferences(): Flow<UserPreferences> = preferencesFlow.asStateFlow()

    override suspend fun getUserPreferences(): UserPreferences = preferencesFlow.value

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        preferencesFlow.update { it.copy(themeMode = themeMode) }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        preferencesFlow.update { it.copy(dynamicColor = enabled) }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesFlow.update { it.copy(isOnboardingCompleted = completed) }
    }

    override suspend fun setLastUsedAccountId(accountId: Long?) {
        preferencesFlow.update { it.copy(lastUsedAccountId = accountId) }
    }
}
