package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing

private val ERROR_STATE_PEBBLE = 72.dp

/**
 * Error message with a single recovery action (retry or go back). Use it when the screen has
 * nothing else to show, e.g. an item opened by id that no longer exists or a failed page load.
 */
@Composable
fun ErrorState(
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TonalPebble(
            icon = Icons.Default.ErrorOutline,
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
            size = ERROR_STATE_PEBBLE,
        )
        Spacer(modifier = Modifier.height(spacing.l))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(spacing.xxl))
        Button(onClick = onActionClick) {
            Text(text = actionLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    WalletTheme(darkTheme = false) {
        ErrorState(
            message = stringResource(R.string.error_not_found),
            actionLabel = stringResource(R.string.action_back),
            onActionClick = {},
        )
    }
}
