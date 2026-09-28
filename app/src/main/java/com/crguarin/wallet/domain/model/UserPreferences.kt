package com.crguarin.wallet.domain.model

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val lastUsedAccountId: Long? = null,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
