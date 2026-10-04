package com.crguarin.wallet.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationPermissionLogicTest {
    @Test
    fun `a denied request that can no longer prompt opens the system settings`() {
        assertThat(
            shouldOpenSettingsAfterRequest(
                sdkInt = 33,
                granted = false,
                shouldShowRequestPermissionRationale = false,
            ),
        ).isTrue()
    }

    @Test
    fun `a granted request does not open the system settings`() {
        assertThat(
            shouldOpenSettingsAfterRequest(
                sdkInt = 33,
                granted = true,
                shouldShowRequestPermissionRationale = false,
            ),
        ).isFalse()
    }

    @Test
    fun `while the system can still prompt it does not open the system settings`() {
        assertThat(
            shouldOpenSettingsAfterRequest(
                sdkInt = 33,
                granted = false,
                shouldShowRequestPermissionRationale = true,
            ),
        ).isFalse()
    }

    @Test
    fun `below API 33 it does not open the system settings after the request`() {
        assertThat(
            shouldOpenSettingsAfterRequest(
                sdkInt = 32,
                granted = false,
                shouldShowRequestPermissionRationale = false,
            ),
        ).isFalse()
    }
}
