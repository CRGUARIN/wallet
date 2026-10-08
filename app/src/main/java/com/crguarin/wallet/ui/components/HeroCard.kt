package com.crguarin.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.ui.theme.WalletBrushes
import com.crguarin.wallet.ui.theme.WalletTheme

/**
 * Headline card with the signature gradient and two soft circles, for the main figure of a screen.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    brush: Brush = WalletBrushes.hero(),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(brush)
                    .drawBehind {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.10f),
                            radius = size.minDimension * 0.9f,
                            center = Offset(size.width, 0f),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.07f),
                            radius = size.minDimension * 0.55f,
                            center = Offset(size.width * 0.82f, size.height),
                        )
                    },
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun HeroCardPreviewLight() {
    WalletTheme(darkTheme = false) {
        HeroCard {
            Text(text = stringResource(R.string.accounts_total_balance), style = MaterialTheme.typography.titleSmall)
            MoneyText(amount = Money(125_000_000L), style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun HeroCardPreviewDark() {
    WalletTheme(darkTheme = true) {
        HeroCard {
            Text(text = stringResource(R.string.accounts_total_balance), style = MaterialTheme.typography.titleSmall)
            MoneyText(amount = Money(125_000_000L), style = MaterialTheme.typography.headlineLarge)
        }
    }
}
