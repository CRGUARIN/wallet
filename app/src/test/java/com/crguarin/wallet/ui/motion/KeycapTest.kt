package com.crguarin.wallet.ui.motion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.DefaultWalletDepth
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class KeycapTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `clicking the face calls onClick`() {
        var clicks = 0
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Keycap(onClick = { clicks += 1 }, face = Color.White, shape = RoundedCornerShape(16.dp)) {
                    Text("Tecla")
                }
            }
        }

        composeRule.onNodeWithText("Tecla").performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `a disabled keycap is not clickable`() {
        var clicks = 0
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Keycap(
                    onClick = { clicks += 1 },
                    face = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    enabled = false,
                ) {
                    Text("Tecla")
                }
            }
        }

        composeRule.onNodeWithText("Tecla").assertIsNotEnabled()

        assertThat(clicks).isEqualTo(0)
    }

    @Test
    fun `the touch area covers at least 48 dp`() {
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Keycap(
                    onClick = {},
                    face = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(52.dp),
                ) {
                    Text("Tecla")
                }
            }
        }

        composeRule.onNode(hasClickAction()).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `long press calls onLongClick when given`() {
        var longs = 0
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Keycap(
                    onClick = {},
                    face = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    role = Role.Button,
                    onLongClick = { longs += 1 },
                ) {
                    Text("Tecla")
                }
            }
        }
        composeRule.onNodeWithText("Tecla").performTouchInput { longClick() }

        assertThat(longs).isEqualTo(1)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun `keys in a wrapping row keep their own width and at least 48 dp of height`() {
        val labels = listOf("Uno", "Dos", "Tres")
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        labels.forEach { label ->
                            Keycap(
                                onClick = {},
                                face = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(text = label, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }

        val rootWidth = composeRule.onRoot().widthPx()
        labels.forEach { label ->
            val keyWidth = composeRule.onNode(hasText(label) and hasClickAction()).widthPx()
            assertThat(keyWidth).isLessThan(rootWidth)
        }
        // The face keeps the 48 dp minimum in an unbounded column; its visible part is 48 minus the depth.
        val faces = composeRule.onAllNodesWithTag(KEYCAP_FACE_TAG, useUnmergedTree = true)
        faces.assertCountEquals(labels.size)
        for (index in labels.indices) {
            faces[index].assertHeightIsAtLeast(48.dp - DefaultWalletDepth.keycapDepth)
        }
    }

    @Test
    fun `a key asked to fill the width keeps the full width and the height it is given`() {
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Keycap(
                        onClick = {},
                        face = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(57.dp),
                    ) {
                        Text("Tecla")
                    }
                }
            }
        }

        val rootWidth = composeRule.onRoot().widthPx()
        assertThat(composeRule.onNode(hasClickAction()).widthPx()).isEqualTo(rootWidth)
        composeRule.onNode(hasClickAction()).assertHeightIsEqualTo(57.dp)
        // The visible face is the key height minus the edge that shows under it.
        composeRule
            .onNodeWithTag(KEYCAP_FACE_TAG, useUnmergedTree = true)
            .assertHeightIsEqualTo(57.dp - DefaultWalletDepth.keycapDepth)
    }

    /** Width in pixels of the layout the interaction finds; the test rule's bounds are in pixels too. */
    private fun SemanticsNodeInteraction.widthPx(): Int = fetchSemanticsNode().size.width
}
