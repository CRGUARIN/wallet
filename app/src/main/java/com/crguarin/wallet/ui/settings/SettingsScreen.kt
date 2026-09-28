package com.crguarin.wallet.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onCategoriesClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.settings_item_categories)) },
                supportingContent = { Text(text = stringResource(R.string.settings_item_categories_subtitle)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.clickable(onClick = onCategoriesClick),
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun SettingsScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        SettingsScreen()
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun SettingsScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        SettingsScreen()
    }
}
