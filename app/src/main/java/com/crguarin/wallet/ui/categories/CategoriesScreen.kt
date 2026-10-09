package com.crguarin.wallet.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.ui.accounts.AddFab
import com.crguarin.wallet.ui.accounts.WalletSegmentedControl
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.PebbleAvatar
import com.crguarin.wallet.ui.components.displayName
import com.crguarin.wallet.ui.components.groupedItemBackground
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.safeHiltViewModel

@Composable
fun CategoriesRoute(
    onNavigateBack: () -> Unit,
    onCategoryClick: (Long, CategoryType) -> Unit,
    onNewCategoryClick: (CategoryType) -> Unit,
    onNewSubcategoryClick: (Long, CategoryType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        CategoriesScreen(
            uiState = CategoriesUiState(),
            onNavigateBack = onNavigateBack,
            onCategoryClick = onCategoryClick,
            onNewCategoryClick = onNewCategoryClick,
            onNewSubcategoryClick = onNewSubcategoryClick,
            onSelectType = {},
            onToggleArchivedExpanded = {},
            onUnarchiveCategory = {},
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CategoriesEvent.ShowSnackbar -> {
                    @Suppress("LocalContextGetResourceValueCall")
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    CategoriesScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onCategoryClick = onCategoryClick,
        onNewCategoryClick = onNewCategoryClick,
        onNewSubcategoryClick = onNewSubcategoryClick,
        onSelectType = viewModel::selectType,
        onToggleArchivedExpanded = viewModel::toggleArchivedExpanded,
        onUnarchiveCategory = viewModel::unarchiveCategory,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    uiState: CategoriesUiState,
    onNavigateBack: () -> Unit,
    onCategoryClick: (Long, CategoryType) -> Unit,
    onNewCategoryClick: (CategoryType) -> Unit,
    onNewSubcategoryClick: (Long, CategoryType) -> Unit,
    onSelectType: (CategoryType) -> Unit,
    onToggleArchivedExpanded: () -> Unit,
    onUnarchiveCategory: (Long) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val spacing = MaterialTheme.walletSpacing
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            AddFab(
                contentDescription = stringResource(R.string.action_new_category),
                onClick = { onNewCategoryClick(uiState.selectedType) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            WalletSegmentedControl(
                options = listOf(CategoryType.EXPENSE, CategoryType.INCOME),
                selected = uiState.selectedType,
                onSelect = onSelectType,
                label = { type ->
                    stringResource(
                        if (type == CategoryType.EXPENSE) R.string.type_expense else R.string.type_income,
                    )
                },
                modifier = Modifier.padding(horizontal = spacing.screenGutter, vertical = spacing.s),
            )

            val isExpense = uiState.selectedType == CategoryType.EXPENSE
            val emptyTitle =
                stringResource(
                    if (isExpense) R.string.categories_empty_expense_title else R.string.categories_empty_income_title,
                )
            val emptyMessage =
                stringResource(
                    if (isExpense) R.string.categories_empty_expense_message else R.string.categories_empty_income_message,
                )

            if (uiState.isLoading) {
                LoadingState(modifier = Modifier.weight(1f))
            } else if (uiState.activeTree.isEmpty() && uiState.archivedCategories.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Category,
                    title = emptyTitle,
                    message = emptyMessage,
                    actionLabel = stringResource(R.string.action_new_category),
                    onActionClick = { onNewCategoryClick(uiState.selectedType) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding =
                        PaddingValues(
                            start = spacing.screenGutter,
                            end = spacing.screenGutter,
                            bottom = FAB_CLEARANCE,
                        ),
                ) {
                    if (uiState.activeTree.isEmpty()) {
                        // Only archived categories exist for this type.
                        item(key = "no_active_categories") {
                            EmptyState(
                                icon = Icons.Default.Category,
                                title = emptyTitle,
                                message = emptyMessage,
                                actionLabel = stringResource(R.string.action_new_category),
                                onActionClick = { onNewCategoryClick(uiState.selectedType) },
                            )
                        }
                    }

                    // A root and its subcategories share one tonal layer.
                    uiState.activeTree.forEachIndexed { groupIndex, rootNode ->
                        val groupSize = 1 + rootNode.children.size
                        item(key = "root_${rootNode.category.id}") {
                            RootCategoryRow(
                                category = rootNode.category,
                                onClick = { onCategoryClick(rootNode.category.id, uiState.selectedType) },
                                onNewSubcategoryClick = {
                                    onNewSubcategoryClick(rootNode.category.id, uiState.selectedType)
                                },
                                modifier =
                                    Modifier
                                        .padding(top = if (groupIndex == 0) 0.dp else spacing.m)
                                        .groupedItemBackground(0, groupSize),
                            )
                        }

                        items(
                            count = rootNode.children.size,
                            key = { "child_${rootNode.children[it].category.id}" },
                        ) { childIndex ->
                            val childNode = rootNode.children[childIndex]
                            SubcategoryRow(
                                category = childNode.category,
                                onClick = { onCategoryClick(childNode.category.id, uiState.selectedType) },
                                modifier = Modifier.groupedItemBackground(childIndex + 1, groupSize),
                            )
                        }
                    }

                    if (uiState.archivedCategories.isNotEmpty()) {
                        item(key = "archived_header") {
                            Surface(
                                onClick = onToggleArchivedExpanded,
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = spacing.l),
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = HEADER_MIN_HEIGHT)
                                            .padding(horizontal = spacing.l, vertical = spacing.s),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text =
                                            pluralStringResource(
                                                R.plurals.categories_archived_count,
                                                uiState.archivedCategories.size,
                                                uiState.archivedCategories.size,
                                            ),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Icon(
                                        imageVector =
                                            if (uiState.isArchivedExpanded) {
                                                Icons.Default.ExpandLess
                                            } else {
                                                Icons.Default.ExpandMore
                                            },
                                        contentDescription =
                                            stringResource(
                                                if (uiState.isArchivedExpanded) {
                                                    R.string.action_collapse
                                                } else {
                                                    R.string.action_expand
                                                },
                                            ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        if (uiState.isArchivedExpanded) {
                            items(
                                count = uiState.archivedCategories.size,
                                key = { "archived_${uiState.archivedCategories[it].id}" },
                            ) { index ->
                                val category = uiState.archivedCategories[index]
                                ArchivedCategoryRow(
                                    category = category,
                                    onClick = { onCategoryClick(category.id, uiState.selectedType) },
                                    onUnarchiveClick = { onUnarchiveCategory(category.id) },
                                    modifier =
                                        Modifier
                                            .padding(top = if (index == 0) spacing.s else 0.dp)
                                            .groupedItemBackground(index, uiState.archivedCategories.size),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val HEADER_MIN_HEIGHT = 56.dp

/** Room under the list so the last row is not covered by the add button. */
private val FAB_CLEARANCE = 96.dp

private val ROOT_AVATAR_SIZE = 44.dp
private val CHILD_AVATAR_SIZE = 36.dp

@Composable
private fun RootCategoryRow(
    category: Category,
    onClick: () -> Unit,
    onNewSubcategoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = spacing.l, end = spacing.s, top = spacing.s, bottom = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PebbleAvatar(
            icon = IconCatalog.get(category.icon),
            color = Color(category.color),
            size = ROOT_AVATAR_SIZE,
        )

        Spacer(modifier = Modifier.width(spacing.m))

        Text(
            text = category.displayName(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )

        TextButton(
            onClick = onNewSubcategoryClick,
            contentPadding = PaddingValues(horizontal = spacing.s, vertical = spacing.xs),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(spacing.xs))
            Text(
                text = stringResource(R.string.action_add_subcategory),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SubcategoryRow(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                // Lined up with the name of the root: gutter, root avatar and the gap after it.
                .padding(
                    start = spacing.l + ROOT_AVATAR_SIZE + spacing.m,
                    end = spacing.l,
                    top = spacing.s,
                    bottom = spacing.s,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PebbleAvatar(
            icon = IconCatalog.get(category.icon),
            color = Color(category.color),
            size = CHILD_AVATAR_SIZE,
        )

        Spacer(modifier = Modifier.width(spacing.m))

        Text(
            text = category.displayName(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ArchivedCategoryRow(
    category: Category,
    onClick: () -> Unit,
    onUnarchiveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = spacing.l, end = spacing.s, top = spacing.xs, bottom = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PebbleAvatar(
            icon = IconCatalog.get(category.icon),
            color = Color(category.color),
            size = CHILD_AVATAR_SIZE,
        )

        Spacer(modifier = Modifier.width(spacing.m))

        Text(
            text = category.displayName(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        TextButton(onClick = onUnarchiveClick) {
            Text(text = stringResource(R.string.action_unarchive))
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun CategoriesScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        CategoriesScreen(
            uiState =
                CategoriesUiState(
                    selectedType = CategoryType.EXPENSE,
                    activeTree =
                        listOf(
                            CategoryNode(
                                category =
                                    Category(
                                        id = 1L,
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
                                                    id = 2L,
                                                    systemKey = "groceries",
                                                    type = CategoryType.EXPENSE,
                                                    parentId = 1L,
                                                    color = 0xFFEF5350.toInt(),
                                                    icon = "shopping_cart",
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                    archivedCategories =
                        listOf(
                            Category(
                                id = 3L,
                                name = "Gasto viejo",
                                type = CategoryType.EXPENSE,
                                color = 0xFF78909C.toInt(),
                                icon = "category",
                                isArchived = true,
                            ),
                        ),
                    isArchivedExpanded = true,
                ),
            onNavigateBack = {},
            onCategoryClick = { _, _ -> },
            onNewCategoryClick = {},
            onNewSubcategoryClick = { _, _ -> },
            onSelectType = {},
            onToggleArchivedExpanded = {},
            onUnarchiveCategory = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun CategoriesScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        CategoriesScreen(
            uiState =
                CategoriesUiState(
                    selectedType = CategoryType.EXPENSE,
                    activeTree =
                        listOf(
                            CategoryNode(
                                category =
                                    Category(
                                        id = 1L,
                                        systemKey = "food",
                                        type = CategoryType.EXPENSE,
                                        color = 0xFFEF5350.toInt(),
                                        icon = "restaurant",
                                    ),
                            ),
                        ),
                ),
            onNavigateBack = {},
            onCategoryClick = { _, _ -> },
            onNewCategoryClick = {},
            onNewSubcategoryClick = { _, _ -> },
            onSelectType = {},
            onToggleArchivedExpanded = {},
            onUnarchiveCategory = {},
        )
    }
}
