package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
