package com.crguarin.wallet.ui.recurring

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.crguarin.wallet.R

/**
 * Provisional stub: Task 16 (recurring UI) implements the list with this route signature
 * (plus defaulted parameters as needed).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListRoute(
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    onPaymentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.recurring_title)) })
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Text(
            text = stringResource(R.string.recurring_title),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
