package com.crguarin.wallet.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.PebbleAvatar
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography

/**
 * "Elige la tarjeta": the transit cards with their name, trips left (or the state of the credit
 * trip) and balance, so a trip can be registered on the right one with a single tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripCardSheet(
    cards: List<AccountWithBalance>,
    onCardSelected: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = spacing.xxl),
        ) {
            SectionHeader(
                title = stringResource(R.string.home_trip_choose_card),
                modifier = Modifier.padding(horizontal = spacing.xxl, vertical = spacing.m),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(cards, key = { it.account.id }) { item ->
                    TripCardRow(item = item, onClick = { onCardSelected(item.account.id) })
                }
            }
        }
    }
}

@Composable
private fun TripCardRow(
    item: AccountWithBalance,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    val account = item.account
    val status = transitStatusText(item.balance, account.fare)
    val needsAttention = transitNeedsAttention(item.balance, account.fare)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.xxl, vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PebbleAvatar(
            icon = IconCatalog.get(account.icon),
            color = Color(account.color),
            size = AccountAvatarSize,
            showRing = true,
            needsAttention = needsAttention,
            base = MaterialTheme.colorScheme.surfaceContainerLow,
        )
        Spacer(modifier = Modifier.width(spacing.m))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (status != null) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (needsAttention) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
        Spacer(modifier = Modifier.width(spacing.m))
        MoneyText(
            amount = item.balance,
            style = MaterialTheme.walletTypography.rowAmount,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
