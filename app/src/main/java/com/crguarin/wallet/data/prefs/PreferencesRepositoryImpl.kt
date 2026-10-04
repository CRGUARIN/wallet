package com.crguarin.wallet.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.model.UserPreferences
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import com.crguarin.wallet.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
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

        private object NotificationKeys {
            val ENABLED = booleanPreferencesKey("notif_enabled")
            val HOUR = intPreferencesKey("notif_hour")
            val MINUTE = intPreferencesKey("notif_minute")
            val RECURRING_REMINDERS = booleanPreferencesKey("notif_recurring_reminders")
            val RECURRING_POSTED = booleanPreferencesKey("notif_recurring_posted")
            val RECURRING_PENDING = booleanPreferencesKey("notif_recurring_pending")
            val CARD_CLOSING = booleanPreferencesKey("notif_card_closing")
            val CARD_CLOSING_DAYS_BEFORE = intPreferencesKey("notif_card_closing_days_before")
            val CARD_PAYMENT = booleanPreferencesKey("notif_card_payment")
            val CARD_PAYMENT_DAYS_BEFORE = intPreferencesKey("notif_card_payment_days_before")
            val CARD_OVERDUE = booleanPreferencesKey("notif_card_overdue")
            val LAST_RUN_DATE = stringPreferencesKey("last_notification_run_date")
        }

        private fun notificationSettingsOf(preferences: Preferences): NotificationSettings {
            val defaults = NotificationSettings()
            return NotificationSettings(
                enabled = preferences[NotificationKeys.ENABLED] ?: defaults.enabled,
                hour = (preferences[NotificationKeys.HOUR] ?: defaults.hour).coerceIn(0, 23),
                minute = (preferences[NotificationKeys.MINUTE] ?: defaults.minute).coerceIn(0, 59),
                recurringReminders = preferences[NotificationKeys.RECURRING_REMINDERS] ?: defaults.recurringReminders,
                recurringPosted = preferences[NotificationKeys.RECURRING_POSTED] ?: defaults.recurringPosted,
                recurringPendingConfirm = preferences[NotificationKeys.RECURRING_PENDING] ?: defaults.recurringPendingConfirm,
                cardClosing = preferences[NotificationKeys.CARD_CLOSING] ?: defaults.cardClosing,
                cardClosingDaysBefore =
                    (preferences[NotificationKeys.CARD_CLOSING_DAYS_BEFORE] ?: defaults.cardClosingDaysBefore)
                        .coerceIn(0, 7),
                cardPayment = preferences[NotificationKeys.CARD_PAYMENT] ?: defaults.cardPayment,
                cardPaymentDaysBefore =
                    (preferences[NotificationKeys.CARD_PAYMENT_DAYS_BEFORE] ?: defaults.cardPaymentDaysBefore)
                        .coerceIn(0, 7),
                cardOverdue = preferences[NotificationKeys.CARD_OVERDUE] ?: defaults.cardOverdue,
            )
        }

        private fun NotificationSettings.clamped(): NotificationSettings =
            copy(
                hour = hour.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59),
                cardClosingDaysBefore = cardClosingDaysBefore.coerceIn(0, 7),
                cardPaymentDaysBefore = cardPaymentDaysBefore.coerceIn(0, 7),
            )

        private fun Preferences.toLastRunDate(): LocalDate? =
            this[NotificationKeys.LAST_RUN_DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        override fun observeNotificationSettings(): Flow<NotificationSettings> =
            dataStore.data
                .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
                .map { preferences -> notificationSettingsOf(preferences) }

        override suspend fun getNotificationSettings(): NotificationSettings = observeNotificationSettings().first()

        override suspend fun updateNotificationSettings(transform: (NotificationSettings) -> NotificationSettings) {
            dataStore.edit { preferences ->
                val updated = transform(notificationSettingsOf(preferences)).clamped()
                preferences[NotificationKeys.ENABLED] = updated.enabled
                preferences[NotificationKeys.HOUR] = updated.hour
                preferences[NotificationKeys.MINUTE] = updated.minute
                preferences[NotificationKeys.RECURRING_REMINDERS] = updated.recurringReminders
                preferences[NotificationKeys.RECURRING_POSTED] = updated.recurringPosted
                preferences[NotificationKeys.RECURRING_PENDING] = updated.recurringPendingConfirm
                preferences[NotificationKeys.CARD_CLOSING] = updated.cardClosing
                preferences[NotificationKeys.CARD_CLOSING_DAYS_BEFORE] = updated.cardClosingDaysBefore
                preferences[NotificationKeys.CARD_PAYMENT] = updated.cardPayment
                preferences[NotificationKeys.CARD_PAYMENT_DAYS_BEFORE] = updated.cardPaymentDaysBefore
                preferences[NotificationKeys.CARD_OVERDUE] = updated.cardOverdue
            }
        }

        override suspend fun getLastNotificationRunDate(): LocalDate? =
            dataStore.data
                .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
                .map { preferences -> preferences.toLastRunDate() }
                .first()

        override suspend fun setLastNotificationRunDate(date: LocalDate) {
            dataStore.edit { preferences ->
                preferences[NotificationKeys.LAST_RUN_DATE] = date.toString()
            }
        }
    }
