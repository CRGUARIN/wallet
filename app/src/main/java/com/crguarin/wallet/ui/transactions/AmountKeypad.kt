package com.crguarin.wallet.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.motion.Keycap
import com.crguarin.wallet.ui.theme.TABULAR_FIGURES
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletColors
import com.crguarin.wallet.ui.theme.walletDepth
import com.crguarin.wallet.ui.transactions.AmountKeypadMath.KeypadKey
import java.util.Locale

private val KeyShape = RoundedCornerShape(16.dp)

/** Face height of a keypad key; the relief ([keyDepth]) sits below it. */
private val KeyFaceHeight = 52.dp
private val KeySpacing = 8.dp
private val KeyTextSize = 24.sp
private val TripleZeroTextSize = 20.sp
private val BackspaceIconSize = 24.dp

private val KeyRows: List<List<KeypadKey>> =
    listOf(
        listOf(KeypadKey.Digit(1), KeypadKey.Digit(2), KeypadKey.Digit(3)),
        listOf(KeypadKey.Digit(4), KeypadKey.Digit(5), KeypadKey.Digit(6)),
        listOf(KeypadKey.Digit(7), KeypadKey.Digit(8), KeypadKey.Digit(9)),
        listOf(KeypadKey.TripleZero, KeypadKey.Digit(0), KeypadKey.Backspace),
    )

/**
 * The movement form's own numeric pad (§9.1): 3 × 4 relieved keys (`1–9`, `000`, `0`, `⌫`) that
 * write through [onAmountChanged] using [AmountKeypadMath], so the form state and its
 * `SavedStateHandle` persistence keep working untouched.
 *
 * The keys deliberately expose no [Text]: each key clears its subtree semantics and only offers a
 * `contentDescription` (the digit, `keypad_triple_zero` or `keypad_delete`) with [Role.Button], so
 * `onNodeWithText("1")` keeps pointing at the installments field. Holding `⌫` clears the whole
 * amount.
 */
@Composable
fun AmountKeypad(
    current: String,
    onAmountChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val depth = MaterialTheme.walletDepth.keyDepth
    val scheme = MaterialTheme.colorScheme
    val face = if (MaterialTheme.walletColors.isDark) scheme.surfaceContainer else scheme.surfaceContainerLowest
    val content = scheme.onSurface
    val haptics = LocalHapticFeedback.current
    val tripleZeroDescription = stringResource(R.string.keypad_triple_zero)
    val deleteDescription = stringResource(R.string.keypad_delete)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KeySpacing)) {
        KeyRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeySpacing)) {
                row.forEach { key ->
                    val description =
                        when (key) {
                            is KeypadKey.Digit -> key.d.toString()
                            KeypadKey.TripleZero -> tripleZeroDescription
                            KeypadKey.Backspace -> deleteDescription
                        }

                    // Physical taps go through Keycap (with its own haptic); TalkBack and tests
                    // go through the cleared semantics below (with a manual haptic). Never both.
                    fun fire(withHaptic: Boolean) {
                        if (withHaptic) haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        onAmountChanged(
                            when (key) {
                                KeypadKey.Backspace -> AmountKeypadMath.backspace(current)
                                is KeypadKey.Digit -> AmountKeypadMath.press(current, key, locale)
                                KeypadKey.TripleZero -> AmountKeypadMath.press(current, key, locale)
                            },
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(KeyFaceHeight + depth)
                                .clearAndSetSemantics {
                                    contentDescription = description
                                    role = Role.Button
                                    onClick {
                                        fire(withHaptic = true)
                                        true
                                    }
                                    if (key == KeypadKey.Backspace) {
                                        onLongClick {
                                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                            onAmountChanged("")
                                            true
                                        }
                                    }
                                },
                    ) {
                        Keycap(
                            onClick = { fire(withHaptic = false) },
                            onLongClick =
                                if (key == KeypadKey.Backspace) {
                                    { onAmountChanged("") }
                                } else {
                                    null
                                },
                            face = face,
                            shape = KeyShape,
                            depth = depth,
                            haptic = HapticFeedbackType.KeyboardTap,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            when (key) {
                                is KeypadKey.Digit ->
                                    Text(
                                        text = key.d.toString(),
                                        style =
                                            TextStyle(
                                                fontSize = KeyTextSize,
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = TABULAR_FIGURES,
                                            ),
                                        color = content,
                                    )
                                KeypadKey.TripleZero ->
                                    Text(
                                        text = "000",
                                        style =
                                            TextStyle(
                                                fontSize = TripleZeroTextSize,
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = TABULAR_FIGURES,
                                            ),
                                        color = content,
                                    )
                                KeypadKey.Backspace ->
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                        contentDescription = null,
                                        tint = content,
                                        modifier = Modifier.size(BackspaceIconSize),
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Keypad Light", showBackground = true, widthDp = 360)
@Composable
private fun AmountKeypadPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        AmountKeypad(
            current = "28000",
            onAmountChanged = {},
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Preview(name = "Keypad Dark", showBackground = true, widthDp = 360)
@Composable
private fun AmountKeypadPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        AmountKeypad(
            current = "",
            onAmountChanged = {},
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Preview(name = "Keypad Large font", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun AmountKeypadPreviewLargeFont() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        AmountKeypad(
            current = "28000",
            onAmountChanged = {},
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
