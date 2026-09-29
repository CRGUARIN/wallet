package com.crguarin.wallet.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.model.UserPreferences
import com.crguarin.wallet.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : PreferencesRepository {
        private object PreferencesKeys {
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
            val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
            val LAST_USED_ACCOUNT_ID = longPreferencesKey("last_used_account_id")
        }

        override fun observeUserPreferences(): Flow<UserPreferences> =
            dataStore.data
                .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
                .map { preferences ->
                    val themeModeString = preferences[PreferencesKeys.THEME_MODE]
                    val themeMode =
                        themeModeString?.let {
                            runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
                        } ?: ThemeMode.SYSTEM
                    val dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: true
                    val onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
                    val lastUsedAccountId = preferences[PreferencesKeys.LAST_USED_ACCOUNT_ID]

                    UserPreferences(
                        themeMode = themeMode,
                        dynamicColor = dynamicColor,
                        isOnboardingCompleted = onboardingCompleted,
                        lastUsedAccountId = lastUsedAccountId,
                    )
                }

        override suspend fun getUserPreferences(): UserPreferences = observeUserPreferences().first()

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME_MODE] = themeMode.name
            }
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
            }
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
            }
        }

        override suspend fun setLastUsedAccountId(accountId: Long?) {
            dataStore.edit { preferences ->
                if (accountId != null) {
                    preferences[PreferencesKeys.LAST_USED_ACCOUNT_ID] = accountId
                } else {
                    preferences.remove(PreferencesKeys.LAST_USED_ACCOUNT_ID)
                }
            }
        }
    }
