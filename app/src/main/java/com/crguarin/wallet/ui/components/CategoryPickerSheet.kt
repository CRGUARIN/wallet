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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.ui.theme.WalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<CategoryNode>,
    onCategorySelected: (Category) -> Unit,
    onNewCategoryClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    selectedCategoryId: Long? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    showNewCategoryAction: Boolean = true,
    allOptionLabel: String? = null,
    onAllOptionSelected: (() -> Unit)? = null,
) {
    var expandedCategoryIds by remember { mutableStateOf(setOf<Long>()) }

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
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.category_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (showNewCategoryAction) {
                    TextButton(onClick = onNewCategoryClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.action_new_category))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (allOptionLabel != null && onAllOptionSelected != null) {
                    item(key = "all_categories") {
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

                            if (selectedCategoryId == null) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.cd_selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                items(categories, key = { it.category.id }) { rootNode ->
                    val isExpanded = expandedCategoryIds.contains(rootNode.category.id)
                    val isSelected = rootNode.category.id == selectedCategoryId

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onCategorySelected(rootNode.category) }
                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .background(Color(rootNode.category.color), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = IconCatalog.get(rootNode.category.icon),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = rootNode.category.displayName(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.cd_selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }

                            if (rootNode.children.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        expandedCategoryIds =
                                            if (isExpanded) {
                                                expandedCategoryIds - rootNode.category.id
                                            } else {
                                                expandedCategoryIds + rootNode.category.id
                                            }
                                    },
                                ) {
                                    Icon(
                                        imageVector =
                                            if (isExpanded) {
                                                Icons.Default.ExpandLess
                                            } else {
                                                Icons.Default.ExpandMore
                                            },
                                        contentDescription =
                                            stringResource(
                                                if (isExpanded) {
                                                    R.string.action_collapse
                                                } else {
                                                    R.string.action_expand
                                                },
                                            ),
                                    )
                                }
                            }
                        }

                        if (isExpanded) {
                            rootNode.children.forEach { childNode ->
                                val isChildSelected = childNode.category.id == selectedCategoryId
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { onCategorySelected(childNode.category) }
                                            .padding(start = 72.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(32.dp)
                                                .background(
                                                    Color(childNode.category.color).copy(alpha = 0.8f),
                                                    CircleShape,
                                                ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = IconCatalog.get(childNode.category.icon),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = childNode.category.displayName(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )

                                    if (isChildSelected) {
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun CategoryPickerSheetPreviewLight() {
    WalletTheme(darkTheme = false) {
        CategoryPickerSheet(
            categories =
                listOf(
                    CategoryNode(
                        category =
                            Category(
                                id = 1,
                                systemKey = "food",
                                type = CategoryType.EXPENSE,
                                color = 0xFFEF5350.toInt(),
                                icon = "restaurant",
                            ),
                        children =
                            listOf(
                                CategoryNode(
                                    category =
                                        Category(
                                            id = 2,
                                            systemKey = "groceries",
                                            type = CategoryType.EXPENSE,
                                            parentId = 1,
                                            color = 0xFFEF5350.toInt(),
                                            icon = "local_grocery_store",
                                        ),
                                ),
                            ),
                    ),
                ),
            selectedCategoryId = 1,
            onCategorySelected = {},
            onNewCategoryClick = {},
            onDismissRequest = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun CategoryPickerSheetPreviewDark() {
    WalletTheme(darkTheme = true) {
        CategoryPickerSheet(
            categories =
                listOf(
                    CategoryNode(
                        category =
                            Category(
                                id = 1,
                                systemKey = "food",
                                type = CategoryType.EXPENSE,
                                color = 0xFFEF5350.toInt(),
                                icon = "restaurant",
                            ),
                    ),
                ),
            selectedCategoryId = 1,
            onCategorySelected = {},
            onNewCategoryClick = {},
            onDismissRequest = {},
        )
    }
}
