package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing

/** Size of the pebble that holds the illustration icon. */
private val EMPTY_STATE_PEBBLE = 96.dp

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
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
            icon = icon,
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            size = EMPTY_STATE_PEBBLE,
        )

        Spacer(modifier = Modifier.height(spacing.l))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (message != null) {
            Spacer(modifier = Modifier.height(spacing.s))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(spacing.xxl))
            Button(onClick = onActionClick) {
                Text(text = actionLabel)
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun EmptyStatePreviewLight() {
    WalletTheme(darkTheme = false) {
        EmptyState(
            icon = Icons.Default.Inbox,
            title = stringResource(R.string.empty_state_default_title),
            message = stringResource(R.string.empty_state_default_message),
            actionLabel = stringResource(R.string.action_add),
            onActionClick = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun EmptyStatePreviewDark() {
    WalletTheme(darkTheme = true) {
        EmptyState(
            icon = Icons.Default.Inbox,
            title = stringResource(R.string.empty_state_default_title),
            message = stringResource(R.string.empty_state_default_message),
        )
    }
}
