package com.crguarin.wallet.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.ui.accounts.FormSectionTitle
import com.crguarin.wallet.ui.accounts.TonalSelector
import com.crguarin.wallet.ui.accounts.WalletSegmentedControl
import com.crguarin.wallet.ui.accounts.WalletTextField
import com.crguarin.wallet.ui.components.BottomActionBar
import com.crguarin.wallet.ui.components.CategoryPickerSheet
import com.crguarin.wallet.ui.components.ColorPicker
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.IconPicker
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.PebbleAvatar
import com.crguarin.wallet.ui.components.PrimaryActionButton
import com.crguarin.wallet.ui.components.displayName
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.safeHiltViewModel

@Composable
fun CategoryEditRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryEditViewModel? = safeHiltViewModel(),
    onCategorySaved: ((Long) -> Unit)? = null,
) {
    if (viewModel == null) {
        CategoryEditScreen(
            uiState = CategoryEditUiState(),
            onNavigateBack = onNavigateBack,
            onNameChange = {},
            onTypeChange = {},
            onParentIdChange = {},
            onColorChange = {},
            onIconChange = {},
            onSaveClick = {},
            onArchiveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmSimpleDelete = {},
            onReassignOptionSelected = {},
            onDismissReassignSheet = {},
            onReassignAndConfirm = {},
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
                is CategoryEditEvent.NavigateBack -> {
                    val savedId = event.savedCategoryId
                    if (savedId != null && onCategorySaved != null) {
                        onCategorySaved(savedId)
                    } else {
                        onNavigateBack()
                    }
                }
                is CategoryEditEvent.ShowSnackbar -> {
                    @Suppress("LocalContextGetResourceValueCall")
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    CategoryEditScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNameChange = viewModel::setName,
        onTypeChange = viewModel::setType,
        onParentIdChange = viewModel::setParentId,
        onColorChange = viewModel::setColor,
        onIconChange = viewModel::setIcon,
        onSaveClick = viewModel::saveCategory,
        onArchiveClick = viewModel::archiveCategory,
        onDeleteClick = viewModel::onDeleteClick,
        onDismissDeleteDialog = viewModel::dismissDeleteDialog,
        onConfirmSimpleDelete = viewModel::confirmSimpleDelete,
        onReassignOptionSelected = viewModel::onReassignOptionSelected,
        onDismissReassignSheet = viewModel::dismissReassignSheet,
        onReassignAndConfirm = viewModel::onReassignAndConfirm,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    uiState: CategoryEditUiState,
    onNavigateBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (CategoryType) -> Unit,
    onParentIdChange: (Long?) -> Unit,
    onColorChange: (Int) -> Unit,
    onIconChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmSimpleDelete: () -> Unit,
    onReassignOptionSelected: () -> Unit,
    onDismissReassignSheet: () -> Unit,
    onReassignAndConfirm: (Long) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var parentDropdownExpanded by remember { mutableStateOf(false) }
    val spacing = MaterialTheme.walletSpacing

    val screenTitle =
        when {
            uiState.isEditMode -> stringResource(R.string.category_edit_title)
            uiState.parentId != null -> stringResource(R.string.category_new_subcategory_title)
            else -> stringResource(R.string.category_new_title)
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.action_archive)) },
                                onClick = {
                                    menuExpanded = false
                                    onArchiveClick()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.action_delete),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!uiState.isLoading && !uiState.notFound) {
                BottomActionBar {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                    PrimaryActionButton(
                        text = stringResource(R.string.action_save),
                        onClick = onSaveClick,
                        enabled = uiState.isSaveEnabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (uiState.notFound) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                ErrorState(
                    message = stringResource(R.string.error_not_found),
                    actionLabel = stringResource(R.string.action_back),
                    onActionClick = onNavigateBack,
                )
            }
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            // Nombre
            WalletTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.category_name_label),
                errorRes = uiState.nameErrorRes,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Tipo
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                FormSectionTitle(text = stringResource(R.string.category_type_label))
                WalletSegmentedControl(
                    options = listOf(CategoryType.EXPENSE, CategoryType.INCOME),
                    selected = uiState.type,
                    onSelect = onTypeChange,
                    enabled = !uiState.isTypeLocked,
                    label = { type ->
                        stringResource(
                            if (type == CategoryType.EXPENSE) R.string.type_expense else R.string.type_income,
                        )
                    },
                )
            }

            // Categoría padre opcional
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                FormSectionTitle(text = stringResource(R.string.category_parent_label))

                val selectedParent = uiState.parentOptions.find { it.id == uiState.parentId }
                val parentText = selectedParent?.displayName() ?: stringResource(R.string.category_parent_none)

                Box(modifier = Modifier.fillMaxWidth()) {
                    TonalSelector(
                        value = parentText,
                        enabled = !uiState.isParentDisabled,
                        onClick = { parentDropdownExpanded = true },
                        leading =
                            selectedParent?.let { parent ->
                                @Composable {
                                    PebbleAvatar(
                                        icon = IconCatalog.get(parent.icon),
                                        color = Color(parent.color),
                                        size = PARENT_AVATAR_SIZE,
                                    )
                                }
                            },
                        trailingIcon = Icons.Default.ArrowDropDown,
                        trailingDescription = stringResource(R.string.cd_parent_category),
                    )

                    DropdownMenu(
                        expanded = parentDropdownExpanded,
                        onDismissRequest = { parentDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f),
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.category_parent_none)) },
                            onClick = {
                                parentDropdownExpanded = false
                                onParentIdChange(null)
                            },
                        )
                        uiState.parentOptions.forEach { option ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    PebbleAvatar(
                                        icon = IconCatalog.get(option.icon),
                                        color = Color(option.color),
                                        size = PARENT_AVATAR_SIZE,
                                        base = MaterialTheme.colorScheme.surfaceContainer,
                                    )
                                },
                                text = { Text(text = option.displayName()) },
                                onClick = {
                                    parentDropdownExpanded = false
                                    onParentIdChange(option.id)
                                },
                            )
                        }
                    }
                }
            }

            // Color
            Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                FormSectionTitle(text = stringResource(R.string.category_color_label))
                // The selected color stays enabled even when another category of this type also uses it.
                val disabledColors = uiState.usedColors - uiState.color
                ColorPicker(
                    selectedColor = uiState.color,
                    onColorSelected = onColorChange,
                    disabledColors = disabledColors,
                )
                if (disabledColors.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.category_color_in_use_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Ícono
            Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                FormSectionTitle(text = stringResource(R.string.category_icon_label))
                IconPicker(
                    selectedIcon = uiState.icon,
                    onIconSelected = onIconChange,
                )
            }

            // Error general
            if (uiState.generalErrorRes != null) {
                Text(
                    text = stringResource(uiState.generalErrorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    CategoryDeleteDialogs(
        state = uiState.deleteDialogState,
        onConfirmSimpleDelete = onConfirmSimpleDelete,
        onArchiveClick = onArchiveClick,
        onReassignOptionSelected = onReassignOptionSelected,
        onDismiss = onDismissDeleteDialog,
    )

    // Sheet de reasignación
    if (uiState.showReassignSheet) {
        CategoryPickerSheet(
            categories = uiState.reassignCategoryNodes,
            onCategorySelected = { onReassignAndConfirm(it.id) },
            onNewCategoryClick = {},
            onDismissRequest = onDismissReassignSheet,
        )
    }
}

private val PARENT_AVATAR_SIZE = 32.dp

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun CategoryEditScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        CategoryEditScreen(
            uiState =
                CategoryEditUiState(
                    name = "Alimentación",
                    type = CategoryType.EXPENSE,
                ),
            onNavigateBack = {},
            onNameChange = {},
            onTypeChange = {},
            onParentIdChange = {},
            onColorChange = {},
            onIconChange = {},
            onSaveClick = {},
            onArchiveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmSimpleDelete = {},
            onReassignOptionSelected = {},
            onDismissReassignSheet = {},
            onReassignAndConfirm = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun CategoryEditScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        CategoryEditScreen(
            uiState =
                CategoryEditUiState(
                    name = "Alimentación",
                    type = CategoryType.EXPENSE,
                ),
            onNavigateBack = {},
            onNameChange = {},
            onTypeChange = {},
            onParentIdChange = {},
            onColorChange = {},
            onIconChange = {},
            onSaveClick = {},
            onArchiveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmSimpleDelete = {},
            onReassignOptionSelected = {},
            onDismissReassignSheet = {},
            onReassignAndConfirm = {},
        )
    }
}
