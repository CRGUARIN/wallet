package com.crguarin.wallet.ui.motion

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class MotionPolicyTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun readPolicies(into: MutableList<MotionPolicy>) {
        into += LocalMotionPolicy.current
    }

    private fun policyOf(policy: MotionPolicy?): MotionPolicy {
        val read = mutableListOf<MotionPolicy>()
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                if (policy == null) {
                    readPolicies(read)
                } else {
                    CompositionLocalProvider(LocalMotionPolicy provides policy) {
                        readPolicies(read)
                    }
                }
                Text("policy")
            }
        }
        composeRule.waitForIdle()
        return read.last()
    }

    @Test
    fun `the default policy disables animations`() {
        val policy = policyOf(null)

        assertThat(policy.animate).isFalse()
        assertThat(policy.screenReader).isFalse()
    }

    @Test
    fun `without a provider nothing animates but depth is on`() {
        var animated = true
        var deep = false
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                animated = animationsOn()
                deep = depthOn()
                Text("policy")
            }
        }
        composeRule.waitForIdle()

        assertThat(animated).isFalse()
        assertThat(deep).isTrue()
    }

    @Test
    fun `an enabled policy animates`() {
        var animated = false
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                animated = animationsOn()
                Text("policy")
            }
        }
        composeRule.waitForIdle()

        assertThat(animated).isTrue()
    }

    @Test
    fun `a screen reader disables depth but not motion`() {
        var animated = false
        var deep = true
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = true)) {
                animated = animationsOn()
                deep = depthOn()
                Text("policy")
            }
        }
        composeRule.waitForIdle()

        assertThat(animated).isTrue()
        assertThat(deep).isFalse()
    }

    @Test
    fun `the flat threshold is 130 percent`() {
        assertThat(FLAT_FONT_SCALE).isEqualTo(1.3f)
    }

    @Test
    fun `spoken feedback service without touch exploration is not a screen reader`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val shadow = Shadows.shadowOf(context.getSystemService(AccessibilityManager::class.java))
        shadow.setTouchExplorationEnabled(false)
        shadow.setEnabledAccessibilityServiceList(
            listOf(
                AccessibilityServiceInfo().apply {
                    feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
                },
            ),
        )

        assertFalse(isScreenReaderOn(context))
    }

    @Test
    fun `touch exploration enabled means screen reader`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Shadows
            .shadowOf(context.getSystemService(AccessibilityManager::class.java))
            .setTouchExplorationEnabled(true)

        assertTrue(isScreenReaderOn(context))
    }
}
