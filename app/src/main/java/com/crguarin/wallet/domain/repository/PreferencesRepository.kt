package com.crguarin.wallet.domain.repository

import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun observeUserPreferences(): Flow<UserPreferences>

    suspend fun getUserPreferences(): UserPreferences

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setLastUsedAccountId(accountId: Long?)
}
