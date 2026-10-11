package com.crguarin.wallet.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crguarin.wallet.ui.motion.LocalIntroFinished
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletColors
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class HeroCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun resetTrace() = HeroContourTrace.reset()

    @After
    fun restoreTrace() = HeroContourTrace.reset()

    @Test
    fun `the contour is eight closed rings around the top right corner`() {
        assertThat(HeroContours.COUNT).isEqualTo(8)

        repeat(HeroContours.COUNT) { index ->
            val ring = HeroContours.ring(index)
            assertThat(ring).hasSize(HeroContours.SEGMENTS + 1)
            // Closed: the last point is the first one again.
            assertThat(ring.last().x).isWithin(0.01f).of(ring.first().x)
            assertThat(ring.last().y).isWithin(0.01f).of(ring.first().y)
            // Centered on (300, 18) of the 328 x 150 reference box.
            val centerX =
                ring
                    .dropLast(1)
                    .map { it.x }
                    .average()
                    .toFloat()
            val centerY =
                ring
                    .dropLast(1)
                    .map { it.y }
                    .average()
                    .toFloat()
            assertThat(centerX).isWithin(1f).of(300f)
            assertThat(centerY).isWithin(1f).of(18f)
        }
    }

    @Test
    fun `the first ring starts at the reference point and the rings grow outwards`() {
        val first = HeroContours.ring(0).first()
        assertThat(first.x).isWithin(0.01f).of(326f)
        assertThat(first.y).isWithin(0.01f).of(18f)

        val meanRadius =
            (0 until HeroContours.COUNT).map { index ->
                HeroContours.ring(index).map { hypot(it.x - 300f, (it.y - 18f) / 0.78f) }.average()
            }
        assertThat(meanRadius).isInStrictOrder()
        // 26 + 17 * k, within the 10% deformation
        meanRadius.forEachIndexed { index, radius ->
            assertThat(radius.toFloat()).isWithin(3f + index).of(26f + 17f * index)
        }
    }

    @Test
    fun `content is drawn in the on hero color`() {
        var contentColor = Color.Unspecified
        var onHero = Color.Unspecified
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                onHero = MaterialTheme.walletColors.onHero
                HeroCard {
                    contentColor = LocalContentColor.current
                    Text(text = "Saldo")
                }
            }
        }

        composeRule.onNodeWithText("Saldo").assertIsDisplayed()
        assertThat(contentColor).isEqualTo(onHero)
    }

    @Test
    fun `a card that does not ask for the animation leaves it for Home`() {
        composeRule.setContent { WalletTheme { HeroCard { Text(text = "Estático") } } }
        composeRule.waitForIdle()

        assertThat(HeroContourTrace.claim()).isTrue()
    }

    @Test
    fun `the first card that asks traces the contours and nobody traces after it`() {
        composeRule.setContent {
            WalletTheme {
                HeroCard(animateContours = true) { Text(text = "Animado") }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Animado").assertIsDisplayed()
        assertThat(HeroContourTrace.claim()).isFalse()
    }

    @Test
    fun `the trace waits for the brand intro to finish`() {
        var introDone by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalIntroFinished provides introDone) {
                WalletTheme {
                    HeroCard(animateContours = true) { Text(text = "Espera") }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Espera").assertIsDisplayed()
        assertThat(HeroContourTrace.claim()).isTrue()

        composeRule.runOnIdle { introDone = true }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Espera").assertIsDisplayed()
        assertThat(HeroContourTrace.claim()).isFalse()
    }
}
