package com.crguarin.wallet.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.displayName
import com.crguarin.wallet.ui.theme.WalletTheme
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
            FloatingActionButton(
                onClick = { onNewCategoryClick(uiState.selectedType) },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_new_category),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            PrimaryTabRow(
                selectedTabIndex = if (uiState.selectedType == CategoryType.EXPENSE) 0 else 1,
            ) {
                Tab(
                    selected = uiState.selectedType == CategoryType.EXPENSE,
                    onClick = { onSelectType(CategoryType.EXPENSE) },
                    text = { Text(text = stringResource(R.string.type_expense)) },
                )
                Tab(
                    selected = uiState.selectedType == CategoryType.INCOME,
                    onClick = { onSelectType(CategoryType.INCOME) },
                    text = { Text(text = stringResource(R.string.type_income)) },
                )
            }

            if (uiState.activeTree.isEmpty() && uiState.archivedCategories.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Category,
                    title =
                        stringResource(
                            if (uiState.selectedType == CategoryType.EXPENSE) {
                                R.string.categories_empty_expense_title
                            } else {
                                R.string.categories_empty_income_title
                            },
                        ),
                    message =
                        stringResource(
                            if (uiState.selectedType == CategoryType.EXPENSE) {
                                R.string.categories_empty_expense_message
                            } else {
                                R.string.categories_empty_income_message
                            },
                        ),
                    actionLabel = stringResource(R.string.action_new_category),
                    onActionClick = { onNewCategoryClick(uiState.selectedType) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    uiState.activeTree.forEach { rootNode ->
                        item(key = "root_${rootNode.category.id}") {
                            RootCategoryRow(
                                category = rootNode.category,
                                onClick = { onCategoryClick(rootNode.category.id, uiState.selectedType) },
                                onNewSubcategoryClick = {
                                    onNewSubcategoryClick(rootNode.category.id, uiState.selectedType)
                                },
                            )
                        }

                        items(
                            items = rootNode.children,
                            key = { "child_${it.category.id}" },
                        ) { childNode ->
                            SubcategoryRow(
                                category = childNode.category,
                                onClick = { onCategoryClick(childNode.category.id, uiState.selectedType) },
                            )
                        }
                    }

                    if (uiState.archivedCategories.isNotEmpty()) {
                        item(key = "archived_header") {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedCard(
                                onClick = onToggleArchivedExpanded,
                                shape = RoundedCornerShape(12.dp),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
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
                                items = uiState.archivedCategories,
                                key = { "archived_${it.id}" },
                            ) { category ->
                                ArchivedCategoryRow(
                                    category = category,
                                    onClick = { onCategoryClick(category.id, uiState.selectedType) },
                                    onUnarchiveClick = { onUnarchiveCategory(category.id) },
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RootCategoryRow(
    category: Category,
    onClick: () -> Unit,
    onNewSubcategoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(category.color)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = IconCatalog.get(category.icon),
                contentDescription = stringResource(R.string.cd_category_icon),
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = category.displayName(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )

        TextButton(
            onClick = onNewSubcategoryClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_add_subcategory),
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
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
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 72.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(category.color).copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = IconCatalog.get(category.icon),
                contentDescription = stringResource(R.string.cd_category_icon),
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = category.displayName(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(category.color).copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = IconCatalog.get(category.icon),
                contentDescription = stringResource(R.string.cd_category_icon),
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = category.displayName(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
