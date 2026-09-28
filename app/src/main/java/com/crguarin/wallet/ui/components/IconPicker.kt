package com.crguarin.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<String> = IconCatalog.keys,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icons.forEach { iconKey ->
            val isSelected = iconKey == selectedIcon
            val shape = RoundedCornerShape(12.dp)

            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(shape)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, shape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                            } else {
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape)
                            },
                        ).clickable { onIconSelected(iconKey) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconCatalog.get(iconKey),
                    contentDescription = stringResource(R.string.cd_icon),
                    tint =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun IconPickerPreviewLight() {
    WalletTheme(darkTheme = false) {
        IconPicker(
            selectedIcon = "restaurant",
            onIconSelected = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun IconPickerPreviewDark() {
    WalletTheme(darkTheme = true) {
        IconPicker(
            selectedIcon = "restaurant",
            onIconSelected = {},
        )
    }
}
