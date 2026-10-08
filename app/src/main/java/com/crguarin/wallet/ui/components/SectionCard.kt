package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletTheme

/**
 * Flat section on a surface-container tone with large rounded corners and no shadow.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
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
