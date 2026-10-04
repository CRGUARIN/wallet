package com.crguarin.wallet.notifications

import androidx.core.os.LocaleListCompat

/**
 * Source of the app locales chosen in Settings. The production implementation reads
 * [androidx.appcompat.app.AppCompatDelegate.getApplicationLocales]; tests substitute
 * fixed locales because that static is only synced by the framework on a real device.
 */
fun interface AppLocaleSource {
    fun locales(): LocaleListCompat
}
