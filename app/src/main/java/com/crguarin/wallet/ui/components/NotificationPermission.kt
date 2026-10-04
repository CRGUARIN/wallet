package com.crguarin.wallet.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Notification permission state: whether notifications can be shown ([granted])
 * and [request] to ask for the permission (or open the system settings when the
 * permission can no longer be requested).
 */
@Stable
class NotificationPermissionState internal constructor(granted: Boolean) {
    var granted: Boolean by mutableStateOf(granted)
        private set

    internal var requester: () -> Unit = {}

    fun request() = requester()

    internal fun update(granted: Boolean) {
        this.granted = granted
    }
}

@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val state = remember { NotificationPermissionState(computeGranted(context)) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            askedOnce = true
            state.update(
                isGranted && NotificationManagerCompat.from(context).areNotificationsEnabled(),
            )
        }

    state.requester = {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> openSystemNotificationSettings(context)
            isPermanentlyDenied(context, askedOnce) -> openSystemNotificationSettings(context)
            else -> {
                askedOnce = true
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        state.update(computeGranted(context))
    }

    return state
}

private fun computeGranted(context: Context): Boolean {
    val systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return systemEnabled
    return systemEnabled &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
}

private fun isPermanentlyDenied(
    context: Context,
    askedOnce: Boolean,
): Boolean {
    if (!askedOnce) return false
    val activity = context.findActivity() ?: return false
    return !ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.POST_NOTIFICATIONS,
    )
}

private fun openSystemNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            if (context.findActivity() == null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    context.startActivity(intent)
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
