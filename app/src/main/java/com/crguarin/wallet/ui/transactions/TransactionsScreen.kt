package com.crguarin.wallet.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R

@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    accountId: Long? = null,
    categoryId: Long? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.transactions_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (accountId != null || categoryId != null) {
            Text(
                text = "accountId=$accountId, categoryId=$categoryId",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
