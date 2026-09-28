package com.crguarin.wallet.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel

/**
 * Returns the Hilt ViewModel, or null in inspection mode (previews and tests that render
 * the navigation graph without a Hilt component). In the running app this always delegates
 * to [hiltViewModel], so a missing component fails loudly instead of showing an empty screen.
 */
@Composable
inline fun <reified VM : ViewModel> safeHiltViewModel(): VM? =
    if (LocalInspectionMode.current) {
        null
    } else {
        hiltViewModel<VM>()
    }
