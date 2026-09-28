package com.crguarin.wallet.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.internal.GeneratedComponentManager
import dagger.hilt.internal.GeneratedComponentManagerHolder

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

fun hasHiltComponent(context: Context): Boolean =
    try {
        val activity = context.findActivity() ?: return false
        when (activity) {
            is GeneratedComponentManagerHolder -> activity.componentManager().generatedComponent() != null
            is GeneratedComponentManager<*> -> activity.generatedComponent() != null
            else -> false
        }
    } catch (_: Throwable) {
        false
    }

@Composable
inline fun <reified VM : ViewModel> safeHiltViewModel(): VM? {
    val context = LocalContext.current
    return if (hasHiltComponent(context)) {
        hiltViewModel<VM>()
    } else {
        null
    }
}
