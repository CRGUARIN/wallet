package com.crguarin.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.ui.theme.WalletBrushes
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletColors
import com.crguarin.wallet.ui.theme.walletTypography

private const val CURRENCY_SYMBOL = '$'

/** Size of the "$" in the hero balance, relative to the digits. */
const val HERO_SYMBOL_SCALE = 0.58f

/**
 * How much the "$" is raised, as a [BaselineShift] multiplier (of the font ascent, not of the font
 * size): 0.25 puts the top of the small "$" level with the top of the digits in Manrope.
 */
const val HERO_SYMBOL_RAISE = 0.25f

/** Smallest size the hero balance shrinks to, in sp (at the default font scale). */
private const val HERO_MIN_FONT_SIZE = 22f

/**
 * [text] with its currency symbol ("$") smaller and raised. The characters do not change, only the
 * style of the symbol and of the space right after it (so the gap shrinks with the symbol), so the
 * text stays the same for screen readers and tests. The symbol is found by character, not by
 * position, because the formatter puts it after a minus sign or at the end depending on the
 * locale. Text without a "$" comes out unstyled.
 *
 * [symbolColor] recolors the symbol (the amount form dims it); unspecified keeps the text color.
 */
fun amountWithSmallSymbol(
    text: String,
    symbolScale: Float = HERO_SYMBOL_SCALE,
    symbolRaise: Float = HERO_SYMBOL_RAISE,
    symbolColor: Color = Color.Unspecified,
): AnnotatedString =
    buildAnnotatedString {
        append(text)
        val symbol = text.indexOf(CURRENCY_SYMBOL)
        if (symbol >= 0) {
            val spaceFollows = symbol + 1 < text.length && text[symbol + 1].isWhitespace()
            addStyle(
                SpanStyle(
                    color = symbolColor,
                    fontSize = symbolScale.em,
                    baselineShift = BaselineShift(symbolRaise),
                ),
                start = symbol,
                end = if (spaceFollows) symbol + 2 else symbol + 1,
            )
        }
    }

/**
 * Main balance of the hero card: Manrope 800, tabular figures, with a small raised "$". It fits on
 * one line by shrinking from 42 sp down to 22 sp, so an amount is never cut off. [text] is the
 * already formatted amount (from `MoneyFormatter`); it is drawn as is.
 *
 * At large font scales the 22 sp floor is held at 22 dp, so even "$ 999.999.999.999" fits at 200%.
 * The caller adds the accessibility description, as with `MoneyText`.
 *
 * @param color usually `MaterialTheme.walletColors.onHero`.
 * @param style defaults to `walletTypography.heroAmount`; its font size is the largest size used.
 */
@Composable
fun HeroAmountText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.walletTypography.heroAmount,
) {
    val annotated = remember(text) { amountWithSmallSymbol(text) }
    val minFontSize =
        with(LocalDensity.current) {
            minOf(HERO_MIN_FONT_SIZE, 22.dp.toSp().value).sp
        }
    // The line height follows the font size while it shrinks.
    val lineHeight =
        if (style.lineHeight.isSp && style.fontSize.isSp) {
            (style.lineHeight.value / style.fontSize.value).em
        } else {
            style.lineHeight
        }
    val fitStyle = style.copy(color = color, lineHeight = lineHeight)
    BasicText(
        text = annotated,
        modifier = modifier,
        style = fitStyle,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = style.fontSize),
    )
}

@Preview(name = "Light Mode", showBackground = true, widthDp = 360)
@Composable
private fun HeroAmountTextPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        HeroAmountPreviewContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true, widthDp = 360)
@Composable
private fun HeroAmountTextPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        HeroAmountPreviewContent()
    }
}

@Composable
private fun HeroAmountPreviewContent() {
    val colors = MaterialTheme.walletColors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(WalletBrushes.hero())
                .padding(22.dp),
    ) {
        HeroAmountText(text = "$ 2.340.500", color = colors.onHero)
        HeroAmountText(text = "$ 999.999.999.999", color = colors.onHero)
        HeroAmountText(text = "-$ 1.120.000", color = colors.onHero)
    }
}
