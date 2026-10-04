package com.crguarin.wallet.domain.repository

import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.model.UserPreferences
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PreferencesRepository {
    fun observeUserPreferences(): Flow<UserPreferences>

    suspend fun getUserPreferences(): UserPreferences

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setLastUsedAccountId(accountId: Long?)

    fun observeNotificationSettings(): Flow<NotificationSettings>

    suspend fun getNotificationSettings(): NotificationSettings

    suspend fun updateNotificationSettings(transform: (NotificationSettings) -> NotificationSettings)

    suspend fun getLastNotificationRunDate(): LocalDate?

    suspend fun setLastNotificationRunDate(date: LocalDate)
}
