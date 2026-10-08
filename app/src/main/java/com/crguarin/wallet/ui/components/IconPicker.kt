package com.crguarin.wallet.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme

private val FEATURED_ICON_KEYS =
    listOf(
        "wallet",
        "savings",
        "credit_card",
        "restaurant",
        "local_grocery_store",
        "directions_bus",
        "directions_car",
        "home",
        "power",
        "medical_services",
        "school",
        "movie",
        "shopping_bag",
        "pets",
        "work",
        "card_giftcard",
        "more_horiz",
    )

@Composable
fun IconPicker(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<String> = IconCatalog.keys,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val featuredKeys = (listOf(selectedIcon) + FEATURED_ICON_KEYS).distinct().filter { it in icons }

    Column(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (expanded) {
            IconCatalog.groups.forEach { group ->
                val groupKeys = group.keys.filter { it in icons }
                if (groupKeys.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(group.titleRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { heading() },
                        )
                        IconFlowRow(
                            iconKeys = groupKeys,
                            selectedIcon = selectedIcon,
                            onIconSelected = onIconSelected,
                        )
                    }
                }
            }
        } else {
            IconFlowRow(
                iconKeys = featuredKeys,
                selectedIcon = selectedIcon,
                onIconSelected = onIconSelected,
            )
        }

        TextButton(onClick = { expanded = !expanded }) {
            Text(
                text = stringResource(if (expanded) R.string.icons_show_less else R.string.icons_show_all),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconFlowRow(
    iconKeys: List<String>,
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        iconKeys.forEach { iconKey ->
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
