package com.crguarin.wallet.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class WalletThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class Tokens(
        val extended: WalletExtendedColors,
        val semantic: WalletSemanticColors,
        val spacing: WalletSpacing,
        val motion: WalletMotion,
        val typography: WalletTypography,
        val primary: androidx.compose.ui.graphics.Color,
        val primaryContainer: androidx.compose.ui.graphics.Color,
        val bodyFont: androidx.compose.ui.text.font.FontFamily?,
        val largeShape: androidx.compose.ui.graphics.Shape,
    )

    @Composable
    private fun readTokens(into: MutableList<Tokens>) {
        into +=
            Tokens(
                extended = MaterialTheme.walletColors,
                semantic = MaterialTheme.walletSemanticColors,
                spacing = MaterialTheme.walletSpacing,
                motion = MaterialTheme.walletMotion,
                typography = MaterialTheme.walletTypography,
                primary = MaterialTheme.colorScheme.primary,
                primaryContainer = MaterialTheme.colorScheme.primaryContainer,
                bodyFont = MaterialTheme.typography.bodyLarge.fontFamily,
                largeShape = MaterialTheme.shapes.large,
            )
    }

    private fun tokensFor(
        darkTheme: Boolean,
        dynamicColor: Boolean,
    ): Tokens {
        val read = mutableListOf<Tokens>()
        composeRule.setContent {
            WalletTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                readTokens(read)
                Text("theme")
            }
        }
        composeRule.waitForIdle()
        return read.last()
    }

    @Test
    fun `fixed light theme provides the Montana tokens`() {
        val tokens = tokensFor(darkTheme = false, dynamicColor = false)

        assertThat(tokens.primary).isEqualTo(PrimaryLight)
        assertThat(tokens.extended).isEqualTo(LightWalletExtendedColors)
        assertThat(tokens.semantic).isEqualTo(LightWalletSemanticColors)
        assertThat(tokens.spacing.screenGutter.value).isEqualTo(16f)
        assertThat(tokens.spacing.cardPadding.value).isEqualTo(20f)
        assertThat(tokens.motion.contour).isEqualTo(700)
        assertThat(tokens.motion.pressScale).isEqualTo(0.97f)
        assertThat(tokens.typography.heroAmount.fontFeatureSettings).isEqualTo("tnum")
        assertThat(tokens.typography.rowAmount.fontFeatureSettings).isEqualTo("tnum")
        assertThat(tokens.typography.formAmount.fontFeatureSettings).isEqualTo("tnum")
        assertThat(tokens.bodyFont).isEqualTo(ManropeFontFamily)
        assertThat(tokens.largeShape).isEqualTo(WalletShapes.large)
    }

    @Test
    fun `fixed dark theme provides the dark tokens`() {
        val tokens = tokensFor(darkTheme = true, dynamicColor = false)

        assertThat(tokens.primary).isEqualTo(PrimaryDark)
        assertThat(tokens.extended).isEqualTo(DarkWalletExtendedColors)
        assertThat(tokens.semantic).isEqualTo(DarkWalletSemanticColors)
        assertThat(tokens.extended.isDark).isTrue()
    }

    @Test
    fun `the motion kit keeps the existing values and exposes the new tokens`() {
        val tokens = tokensFor(darkTheme = false, dynamicColor = false)

        assertThat(tokens.motion.fast).isEqualTo(150)
        assertThat(tokens.motion.medium).isEqualTo(220)
        assertThat(tokens.motion.slow).isEqualTo(450)
        assertThat(tokens.motion.contour).isEqualTo(700)
        assertThat(tokens.motion.pressScale).isEqualTo(0.97f)
        assertThat(tokens.motion.pressMillis).isEqualTo(100)
        assertThat(tokens.motion.stagger).isEqualTo(40)
        assertThat(tokens.motion.dealStagger).isEqualTo(80)
        assertThat(tokens.motion.digitStagger).isEqualTo(50)
        assertThat(tokens.motion.idleLoopWindow).isEqualTo(10_000)
        assertThat(tokens.motion.floatPeriod).isEqualTo(6_000)
        assertThat(tokens.motion.sheenPeriod).isEqualTo(7_000)
        assertThat(tokens.motion.pulseCount).isEqualTo(3)
        assertThat(tokens.motion.pulseMillis).isEqualTo(480)
        assertThat(tokens.motion.pulseGap).isEqualTo(120)
        assertThat(tokens.motion.emphasized).isEqualTo(CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))
        assertThat(tokens.motion.roll).isEqualTo(CubicBezierEasing(0.15f, 0.8f, 0.2f, 1f))
    }

    @Test
    fun `the theme provides the depth tokens`() {
        val depth = mutableListOf<WalletDepth>()
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                depth += MaterialTheme.walletDepth
                Text("theme")
            }
        }
        composeRule.waitForIdle()

        val tokens = depth.last()
        assertThat(tokens.deckTilt).isEqualTo(24f)
        assertThat(tokens.deckStrip).isEqualTo(54.dp)
        assertThat(tokens.deckCardHeight).isEqualTo(190.dp)
        assertThat(tokens.deckCardRadius).isEqualTo(22.dp)
        assertThat(tokens.deckCameraDistance).isEqualTo(12f)
        assertThat(tokens.heroTilt).isEqualTo(8f)
        assertThat(tokens.heroCardHeight).isEqualTo(190.dp)
        assertThat(tokens.heroFloorShadow).isEqualTo(18.dp)
        assertThat(tokens.floatAmplitude).isEqualTo(6.dp)
        assertThat(tokens.dealOffsetY).isEqualTo(90.dp)
        assertThat(tokens.dealExtraTilt).isEqualTo(30f)
        assertThat(tokens.dealStartScale).isEqualTo(1.08f)
        assertThat(tokens.exitOffsetY).isEqualTo(90.dp)
        assertThat(tokens.contentShiftY).isEqualTo(16.dp)
        assertThat(tokens.keycapDepth).isEqualTo(5.dp)
        assertThat(tokens.keyDepth).isEqualTo(4.dp)
        assertThat(tokens.parallaxMax).isEqualTo(12f)
    }

    @Test
    fun `dynamic color derives the light hero from the scheme and keeps the rest of the tokens`() {
        assertDynamicTokens(dark = false)
    }

    @Test
    fun `dynamic color derives the dark hero from the scheme and keeps the rest of the tokens`() {
        assertDynamicTokens(dark = true)
    }

    private fun assertDynamicTokens(dark: Boolean) {
        val tokens = tokensFor(darkTheme = dark, dynamicColor = true)
        val expected = dynamicHeroColors(tokens.primary, tokens.primaryContainer, dark)

        assertThat(tokens.extended.heroStart).isEqualTo(expected.start)
        assertThat(tokens.extended.heroEnd).isEqualTo(expected.end)
        assertThat(tokens.extended.onHero).isEqualTo(expected.onHero)
        assertThat(contrastRatio(tokens.extended.onHero, tokens.extended.heroStart)).isAtLeast(4.5f)
        assertThat(contrastRatio(tokens.extended.onHero, tokens.extended.heroEnd)).isAtLeast(4.5f)
        assertThat(tokens.semantic).isEqualTo(if (dark) DarkWalletSemanticColors else LightWalletSemanticColors)
        assertThat(tokens.bodyFont).isEqualTo(ManropeFontFamily)
        assertThat(tokens.largeShape).isEqualTo(WalletShapes.large)
        assertThat(tokens.extended.attentionStart).isEqualTo(if (dark) TertiaryDark else TertiaryLight)
    }
}
