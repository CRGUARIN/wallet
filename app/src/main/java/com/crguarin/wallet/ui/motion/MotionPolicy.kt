package com.crguarin.wallet.ui.motion

import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Which animations may run. The default (no provider) disables everything so previews and plain
 * screen tests stay deterministic and fast; tests that verify motion provide
 * `MotionPolicy(animate = true, …)` by hand. Production provides the real policy in
 * `WalletAppContent`.
 */
@Immutable
data class MotionPolicy(
    val animate: Boolean,
    val screenReader: Boolean,
)

val LocalMotionPolicy = staticCompositionLocalOf { MotionPolicy(animate = false, screenReader = false) }

/** Font scale above which everything 3D falls back to the flat layout. */
const val FLAT_FONT_SCALE = 1.3f

/** True when animations may run at all (`LocalMotionPolicy.current.animate`). */
@Composable
@ReadOnlyComposable
fun animationsOn(): Boolean = LocalMotionPolicy.current.animate

/**
 * True when 3D depth (tilts, deck, coverflow, shared flight) may be used: no screen reader and a
 * font scale of at most [FLAT_FONT_SCALE]. The 2D motion layer (rolling, staggering) only needs
 * [animationsOn].
 */
@Composable
@ReadOnlyComposable
fun depthOn(): Boolean {
    val policy = LocalMotionPolicy.current
    return !policy.screenReader && LocalDensity.current.fontScale <= FLAT_FONT_SCALE
}

/**
 * The policy of the device: `animate` is false with the system "remove animations" setting
 * (animator duration scale 0) and `screenReader` follows touch exploration. Both are re-read on
 * every resume. Needs no new permissions.
 */
@Composable
fun rememberSystemMotionPolicy(): MotionPolicy {
    val context = LocalContext.current
    var animate by remember { mutableStateOf(readAnimatorScale(context) != 0f) }
    var screenReader by remember { mutableStateOf(isScreenReaderOn(context)) }

    DisposableEffect(context) {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val listener =
            AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
                screenReader = enabled
            }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    animate = readAnimatorScale(context) != 0f
                    screenReader = isScreenReaderOn(context)
                }
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return remember(animate, screenReader) { MotionPolicy(animate, screenReader) }
}

private fun readAnimatorScale(context: android.content.Context): Float =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )

/**
 * Only touch exploration counts as a screen reader: some OEM system services declare spoken
 * feedback without being screen readers, so checking the enabled service list gives false positives.
 */
internal fun isScreenReaderOn(context: android.content.Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
    return manager.isTouchExplorationEnabled
}
