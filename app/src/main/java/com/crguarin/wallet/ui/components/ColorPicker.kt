package com.crguarin.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletPalette
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.contentColorOver
import com.crguarin.wallet.ui.theme.walletSpacing

private const val DISABLED_ALPHA = 0.35f

/** Touch target of a swatch; the colored circle inside is smaller. */
private val SWATCH_TOUCH_SIZE = 48.dp
private val SWATCH_SIZE = 40.dp

/**
 * Color swatches. [disabledColors] are shown dimmed and crossed out, and cannot be picked; the
 * selected color is never disabled. The swatches show the pure palette colors, since choosing one
 * means seeing it as it will be (the rest of the app shows them as tints).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    disabledColors: Set<Int> = emptySet(),
    colors: List<Int> = WalletPalette.colors,
) {
    val colorLabel = stringResource(R.string.cd_color)
    val colorInUseLabel = stringResource(R.string.cd_color_in_use)
    val crossColor = MaterialTheme.colorScheme.onSurface
    val selectedBorder = MaterialTheme.colorScheme.onSurface

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.walletSpacing.xxs, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.walletSpacing.xxs),
    ) {
        colors.forEach { colorInt ->
            val isSelected = colorInt == selectedColor
            val isDisabled = !isSelected && colorInt in disabledColors
            val color = Color(colorInt)

            Box(
                modifier =
                    Modifier
                        .size(SWATCH_TOUCH_SIZE)
                        .semantics {
                            contentDescription = if (isDisabled) colorInUseLabel else colorLabel
                            if (isDisabled) disabled()
                        }.clickable(enabled = !isDisabled) { onColorSelected(colorInt) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(SWATCH_SIZE)
                            // Before the background so the swatch is dimmed and the line drawn on top is not.
                            .then(
                                if (isDisabled) {
                                    Modifier.crossedOut(crossColor)
                                } else {
                                    Modifier
                                },
                            ).clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, selectedBorder, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.cd_selected),
                            tint = contentColorOver(color),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Dims what follows in the chain and draws a full-opacity diagonal line over it. */
private fun Modifier.crossedOut(lineColor: Color): Modifier =
    drawWithContent {
        drawContent()
        val inset = size.width * 0.25f
        drawLine(
            color = lineColor,
            start = Offset(inset, size.height - inset),
            end = Offset(size.width - inset, inset),
            strokeWidth = 2.dp.toPx(),
        )
    }.alpha(DISABLED_ALPHA)

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun ColorPickerPreviewLight() {
    WalletTheme(darkTheme = false) {
        ColorPicker(
            selectedColor = WalletPalette.colors.first(),
            onColorSelected = {},
            disabledColors = setOf(0xFFC62828.toInt(), 0xFF42A5F5.toInt()),
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun ColorPickerPreviewDark() {
    WalletTheme(darkTheme = true) {
        ColorPicker(
            selectedColor = WalletPalette.colors.first(),
            onColorSelected = {},
            disabledColors = setOf(0xFFC62828.toInt(), 0xFF42A5F5.toInt()),
        )
    }
}
