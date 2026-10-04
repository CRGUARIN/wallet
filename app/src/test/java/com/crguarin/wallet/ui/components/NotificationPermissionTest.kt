package com.crguarin.wallet.ui.components

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Shared checks for [rememberNotificationPermissionState]; subclasses pin the SDK because
 * the permission only exists on API 33+.
 */
abstract class BaseNotificationPermissionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    protected fun setSystemNotificationsEnabled(enabled: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowOf(notificationManager).setNotificationsEnabled(enabled)
    }

    protected fun observeGranted(): NotificationPermissionState {
        lateinit var state: NotificationPermissionState
        composeRule.setContent {
            state = rememberNotificationPermissionState()
            TextButton(onClick = { state.request() }) {
                Text("Request")
            }
        }
        return state
    }

    @Test
    fun `granted is true by default`() {
        val state = observeGranted()

        assertThat(state.granted).isTrue()
    }

    @Test
    fun `granted is false when the system blocks notifications`() {
        setSystemNotificationsEnabled(false)

        val state = observeGranted()

        assertThat(state.granted).isFalse()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class NotificationPermissionApi32Test : BaseNotificationPermissionTest() {
    @Test
    fun `request opens the system notification settings`() {
        observeGranted()
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeRule.onNodeWithText("Request").performClick()

        val started = shadowOf(composeRule.activity).nextStartedActivity
        assertThat(started.action).isEqualTo(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        assertThat(started.getStringExtra(Settings.EXTRA_APP_PACKAGE)).isEqualTo(context.packageName)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationPermissionApi35Test : BaseNotificationPermissionTest() {
    @Before
    fun grantPermissionByDefault() {
        // Robolectric denies runtime permissions unless granted; the app default is "asked once granted".
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun `granted is false when POST_NOTIFICATIONS is denied`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val state = observeGranted()

        assertThat(state.granted).isFalse()
    }

    @Test
    fun `request asks for the runtime permission instead of opening settings`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        observeGranted()

        composeRule.onNodeWithText("Request").performClick()

        assertThat(shadowOf(composeRule.activity).lastRequestedPermission.requestedPermissions.toList())
            .contains(Manifest.permission.POST_NOTIFICATIONS)
        // The permission controller starts its own intent; settings must not open.
        assertThat(shadowOf(composeRule.activity).peekNextStartedActivity()?.action)
            .isNotEqualTo(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    }
}
