package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.crguarin.wallet.ui.motion.raisedSurface
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing

/**
 * Elevated section with large rounded corners and a soft shadow ([raisedSurface]), no border.
 * The padding is `walletSpacing.cardPadding` unless [contentPadding] says otherwise.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.walletSpacing.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .raisedSurface(MaterialTheme.shapes.large)
                .padding(contentPadding),
        content = content,
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun SectionCardPreviewLight() {
    WalletTheme(darkTheme = false) {
        SectionCard {
            Text(text = "Section", style = MaterialTheme.typography.titleMedium)
            Text(text = "Body text", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun SectionCardPreviewDark() {
    WalletTheme(darkTheme = true) {
        SectionCard {
            Text(text = "Section", style = MaterialTheme.typography.titleMedium)
            Text(text = "Body text", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
