package com.crguarin.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.ui.accounts.CardRowBalance
import com.crguarin.wallet.ui.theme.WalletTheme
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    accounts: List<AccountWithBalance>,
    onAccountSelected: (AccountWithBalance) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    selectedAccountId: Long? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    allOptionLabel: String? = null,
    onAllOptionSelected: (() -> Unit)? = null,
    cardOverviews: Map<Long, CardOverview> = emptyMap(),
    today: LocalDate = LocalDate.now(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.account_picker_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (allOptionLabel != null && onAllOptionSelected != null) {
                    item(key = "all_accounts") {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onAllOptionSelected() }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = allOptionLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )

                            if (selectedAccountId == null) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.cd_selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                items(accounts, key = { it.account.id }) { item ->
                    val isSelected = item.account.id == selectedAccountId
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAccountSelected(item)
                                }.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .background(Color(item.account.color), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = IconCatalog.get(item.account.icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = item.account.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val cardOverview = cardOverviews[item.account.id]
                            if (item.account.type == AccountType.CREDIT_CARD && cardOverview != null) {
                                CardRowBalance(
                                    overview = cardOverview,
                                    today = today,
                                )
                            } else {
                                MoneyText(
                                    amount = item.balance,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.cd_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun AccountPickerSheetPreviewLight() {
    WalletTheme(darkTheme = false) {
        AccountPickerSheet(
            accounts =
                listOf(
                    AccountWithBalance(
                        account =
                            Account(
                                id = 1,
                                name = "Efectivo",
                                type = AccountType.CASH,
                                initialBalance = Money(0),
                                color = 0xFF42A5F5.toInt(),
                                icon = "wallet",
                                createdAt = Instant.EPOCH,
                            ),
                        balance = Money(5000000),
                    ),
                    AccountWithBalance(
                        account =
                            Account(
                                id = 2,
                                name = "Ahorros",
                                type = AccountType.SAVINGS,
                                initialBalance = Money(0),
                                color = 0xFF66BB6A.toInt(),
                                icon = "savings",
                                createdAt = Instant.EPOCH,
                            ),
                        balance = Money(15000000),
                    ),
                ),
            selectedAccountId = 1,
            onAccountSelected = {},
            onDismissRequest = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun AccountPickerSheetPreviewDark() {
    WalletTheme(darkTheme = true) {
        AccountPickerSheet(
            accounts =
                listOf(
                    AccountWithBalance(
                        account =
                            Account(
                                id = 1,
                                name = "Efectivo",
                                type = AccountType.CASH,
                                initialBalance = Money(0),
                                color = 0xFF42A5F5.toInt(),
                                icon = "wallet",
                                createdAt = Instant.EPOCH,
                            ),
                        balance = Money(5000000),
                    ),
                ),
            selectedAccountId = 1,
            onAccountSelected = {},
            onDismissRequest = {},
        )
    }
}
