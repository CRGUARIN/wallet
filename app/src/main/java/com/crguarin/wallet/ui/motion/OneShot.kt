package com.crguarin.wallet.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * False while the brand intro covers the screen, true after it. `HeroCard` waits for it before
 * tracing its contour lines, so the trace is not hidden under the intro.
 */
val LocalIntroFinished = compositionLocalOf { true }

/**
 * Process-wide memory of one-shot effects keyed by name: the first caller to claim a key in the
 * process wins, like [com.crguarin.wallet.ui.components.HeroContourTrace]. Only for effects that
 * must play once per process (the Home deal); per-entry effects use [rememberPlayedOnce].
 */
internal object FirstShowGate {
    private val claimed = mutableSetOf<String>()

    /** True for the first caller of [key] in the process, false for everyone after. */
    fun claim(key: String): Boolean {
        if (claimed.contains(key)) return false
        claimed += key
        return true
    }

    /** True once [key] was claimed in this process. */
    fun isClaimed(key: String): Boolean = claimed.contains(key)

    /** Lets the effects play again. Only for tests. */
    fun reset() {
        claimed.clear()
    }
}

/**
 * Whether the one-shot effect of [key] already played in this back-stack entry. Starts false and
 * survives rotation and going to another screen and back (`rememberSaveable`); the caller sets it
 * to true when the effect starts.
 */
@Composable
fun rememberPlayedOnce(key: String): MutableState<Boolean> = rememberSaveable(key = "played:$key") { mutableStateOf(false) }
