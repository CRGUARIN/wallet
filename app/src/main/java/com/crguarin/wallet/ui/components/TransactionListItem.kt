package com.crguarin.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.theme.WalletTheme
import java.time.Instant
import java.time.LocalDate

/**
 * Single transaction row shared by the transactions list, the account detail and Home.
 *
 * @param perspectiveAccountId when set (account detail or account filter), a transfer is shown
 *   with the sign seen from that account: outgoing `−`, incoming `+`.
 */
@Composable
fun TransactionListItem(
    details: TransactionDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    perspectiveAccountId: Long? = null,
) {
    val transaction = details.transaction
    val isTransfer = transaction.type == TransactionType.TRANSFER
    val title =
        if (isTransfer) {
            stringResource(R.string.type_transfer)
        } else {
            categoryFullDisplayName(
                parentName = details.parentCategoryName,
                parentSystemKey = details.parentCategorySystemKey,
                name = details.categoryName,
                systemKey = details.categorySystemKey,
            ).ifBlank { stringResource(transaction.type.labelRes()) }
        }

    val subtitle =
        if (isTransfer) {
            stringResource(
                R.string.transaction_transfer_accounts,
                details.accountName,
                details.toAccountName.orEmpty(),
            )
        } else {
            details.accountName
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransactionAvatar(
            details = details,
            isTransfer = isTransfer,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (transaction.recurringId != null) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = stringResource(R.string.recurring_icon_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!details.transaction.note.isNullOrBlank()) {
                Text(
                    text = details.transaction.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        TransactionAmount(
            details = details,
            perspectiveAccountId = perspectiveAccountId,
        )
    }
}

@Composable
private fun TransactionAvatar(
    details: TransactionDetails,
    isTransfer: Boolean,
) {
    val containerColor =
        if (isTransfer) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            Color(details.categoryColor ?: details.accountColor)
        }
    val tintColor =
        if (isTransfer) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color.White
        }

    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector =
                if (isTransfer) {
                    Icons.Default.SwapHoriz
                } else {
                    IconCatalog.get(details.categoryIcon ?: IconCatalog.DEFAULT_ICON_KEY)
                },
            // Decorative: the row already reads the category name and the typed amount.
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun TransactionAmount(
    details: TransactionDetails,
    perspectiveAccountId: Long?,
) {
    val transaction = details.transaction
    val sign =
        if (transaction.type == TransactionType.TRANSFER && perspectiveAccountId != null) {
            when (perspectiveAccountId) {
                transaction.accountId -> MoneySign.MINUS
                transaction.toAccountId -> MoneySign.PLUS
                else -> MoneySign.AUTO
            }
        } else {
            MoneySign.AUTO
        }

    MoneyText(
        amount = transaction.amount,
        type = transaction.type,
        sign = sign,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

private fun TransactionType.labelRes(): Int =
    when (this) {
        TransactionType.INCOME -> R.string.type_income
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.TRANSFER -> R.string.type_transfer
    }

private val previewDate: LocalDate = LocalDate.of(2026, 9, 28)

private fun sampleDetails(
    type: TransactionType,
    categoryName: String? = null,
    categorySystemKey: String? = null,
    parentCategoryName: String? = null,
    parentCategorySystemKey: String? = null,
    accountId: Long = 1L,
    toAccountId: Long? = null,
    note: String? = null,
): TransactionDetails =
    TransactionDetails(
        transaction =
            Transaction(
                id = 1L,
                type = type,
                amount = Money(4_550_000L),
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = if (type == TransactionType.TRANSFER) null else 10L,
                date = previewDate,
                note = note,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        accountName = "Efectivo",
        accountColor = 0xFF66BB6A.toInt(),
        accountIcon = "wallet",
        toAccountName = if (type == TransactionType.TRANSFER) "Ahorros" else null,
        toAccountColor = if (type == TransactionType.TRANSFER) 0xFF42A5F5.toInt() else null,
        toAccountIcon = if (type == TransactionType.TRANSFER) "savings" else null,
        categoryName = categoryName,
        categorySystemKey = categorySystemKey,
        categoryColor = if (type == TransactionType.TRANSFER) null else 0xFFEF5350.toInt(),
        categoryIcon = if (type == TransactionType.TRANSFER) null else "restaurant",
        categoryParentId = if (parentCategoryName != null) 1L else null,
        parentCategoryName = parentCategoryName,
        parentCategorySystemKey = parentCategorySystemKey,
    )

@Preview(name = "Transaction Item Light", showBackground = true)
@Composable
private fun TransactionListItemPreviewLight() {
    WalletTheme(darkTheme = false) {
        Column {
            TransactionListItem(
                details =
                    sampleDetails(
                        type = TransactionType.EXPENSE,
                        categorySystemKey = "groceries",
                        parentCategorySystemKey = "food",
                        note = "Mercado de la semana",
                    ),
                onClick = {},
            )
            TransactionListItem(
                details = sampleDetails(type = TransactionType.INCOME, categorySystemKey = "salary"),
                onClick = {},
            )
            TransactionListItem(
                details = sampleDetails(type = TransactionType.TRANSFER),
                onClick = {},
                perspectiveAccountId = 1L,
            )
        }
    }
}

@Preview(name = "Transaction Item Dark", showBackground = true)
@Composable
private fun TransactionListItemPreviewDark() {
    WalletTheme(darkTheme = true) {
        Column {
            TransactionListItem(
                details = sampleDetails(type = TransactionType.EXPENSE, categorySystemKey = "food"),
                onClick = {},
            )
            TransactionListItem(
                details = sampleDetails(type = TransactionType.TRANSFER),
                onClick = {},
                perspectiveAccountId = 2L,
            )
        }
    }
}
