package com.crguarin.wallet.ui.categories

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.crguarin.wallet.R
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNameMatcher
import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.SystemCategoryNames
import com.crguarin.wallet.domain.model.buildCategoryTree
import com.crguarin.wallet.domain.model.card.CardSystemCategories
import com.crguarin.wallet.domain.model.normalizedName
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.ArchiveCategoryUseCase
import com.crguarin.wallet.domain.usecase.DeleteCategoryUseCase
import com.crguarin.wallet.domain.usecase.ReassignAndDeleteCategoryUseCase
import com.crguarin.wallet.domain.usecase.SaveCategoryUseCase
import com.crguarin.wallet.ui.navigation.CategoryEdit
import com.crguarin.wallet.ui.theme.WalletPalette
import com.crguarin.wallet.ui.util.asStringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DeleteDialogState {
    data object None : DeleteDialogState

    data object ConfirmSimple : DeleteDialogState

    data object HasChildren : DeleteDialogState

    data class HasTransactions(
        val count: Int,
    ) : DeleteDialogState
}

data class CategoryEditUiState(
    val isLoading: Boolean = false,
    /** Edit mode for an id that no longer exists. */
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val categoryId: Long? = null,
    val name: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val isTypeLocked: Boolean = false,
    val parentId: Long? = null,
    val isParentDisabled: Boolean = false,
    val parentOptions: List<Category> = emptyList(),
    val color: Int = WalletPalette.colors.first(),
    val icon: String = "restaurant",
    val isSaving: Boolean = false,
    val nameErrorRes: Int? = null,
    val generalErrorRes: Int? = null,
    val deleteDialogState: DeleteDialogState = DeleteDialogState.None,
    val showReassignSheet: Boolean = false,
    val reassignCategoryNodes: List<CategoryNode> = emptyList(),
    val systemKey: String? = null,
) {
    val isSaveEnabled: Boolean
        get() =
            !isLoading &&
                !isSaving &&
                name.isNotBlank() &&
                name.trim().length <= DomainLimits.NAME_MAX_LENGTH &&
                nameErrorRes == null
}

sealed interface CategoryEditEvent {
    data class NavigateBack(
        val savedCategoryId: Long? = null,
    ) : CategoryEditEvent

    data class ShowSnackbar(
        @StringRes val messageRes: Int,
    ) : CategoryEditEvent
}

@HiltViewModel
class CategoryEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val categoryRepository: CategoryRepository,
        private val recurringRepository: RecurringRepository,
        private val saveCategoryUseCase: SaveCategoryUseCase,
        private val deleteCategoryUseCase: DeleteCategoryUseCase,
        private val reassignAndDeleteCategoryUseCase: ReassignAndDeleteCategoryUseCase,
        private val archiveCategoryUseCase: ArchiveCategoryUseCase,
        private val systemCategoryNames: SystemCategoryNames,
        private val categoryNameMatcher: CategoryNameMatcher,
    ) : ViewModel() {
        private val route: CategoryEdit = savedStateHandle.toRoute<CategoryEdit>()
        val targetCategoryId: Long? = route.categoryId
        private val initialParentId: Long? = route.parentId
        private val initialType: CategoryType = route.type

        private val _uiState = MutableStateFlow(CategoryEditUiState(isLoading = true))
        val uiState: StateFlow<CategoryEditUiState> = _uiState.asStateFlow()

        private val _events = Channel<CategoryEditEvent>(Channel.BUFFERED)
        val events: Flow<CategoryEditEvent> = _events.receiveAsFlow()

        private var existingCategories: List<Category> = emptyList()
        var preloadedSystemName: String? = null
            private set

        init {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                existingCategories = categoryRepository.getAllCategories()

                if (targetCategoryId != null && targetCategoryId > 0) {
                    val category = categoryRepository.getCategory(targetCategoryId)
                    if (category != null) {
                        val childrenCount = categoryRepository.countChildren(targetCategoryId)
                        val txCount = categoryRepository.countTransactions(targetCategoryId)
                        val isTypeLocked =
                            category.parentId != null ||
                                txCount > 0 ||
                                category.systemKey in CardSystemCategories.KEYS ||
                                recurringRepository.countByCategory(targetCategoryId) > 0
                        val isParentDisabled = childrenCount > 0

                        val resolvedName =
                            if (category.name != null) {
                                category.name
                            } else if (category.systemKey != null) {
                                systemCategoryNames.currentNameFor(category.systemKey) ?: ""
                            } else {
                                ""
                            }

                        if (category.systemKey != null && category.name == null) {
                            preloadedSystemName = resolvedName
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isEditMode = true,
                                categoryId = category.id,
                                name = resolvedName,
                                type = category.type,
                                isTypeLocked = isTypeLocked,
                                parentId = category.parentId,
                                isParentDisabled = isParentDisabled,
                                color = category.color,
                                icon = category.icon,
                                systemKey = category.systemKey,
                            )
                        }
                        refreshParentOptions(category.type, category.id)
                    } else {
                        _uiState.update { it.copy(isLoading = false, notFound = true) }
                    }
                } else {
                    var initialColor = WalletPalette.colors.first()
                    if (initialParentId != null) {
                        val parent = categoryRepository.getCategory(initialParentId)
                        if (parent != null) {
                            initialColor = parent.color
                        }
                    }
                    val defaultIcon = if (initialType == CategoryType.EXPENSE) "restaurant" else "work"
                    val isTypeLocked = initialParentId != null

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = false,
                            type = initialType,
                            isTypeLocked = isTypeLocked,
                            parentId = initialParentId,
                            color = initialColor,
                            icon = defaultIcon,
                        )
                    }
                    refreshParentOptions(initialType, null)
                }
            }
        }

        private fun refreshParentOptions(
            type: CategoryType,
            excludeId: Long?,
        ) {
            val activeRoots =
                existingCategories.filter {
                    it.type == type && it.parentId == null && !it.isArchived && it.id != excludeId
                }
            _uiState.update { it.copy(parentOptions = activeRoots) }
        }

        fun setName(name: String) {
            val trimmed = name.trim()
            val state = _uiState.value
            val error =
                when {
                    trimmed.length > DomainLimits.NAME_MAX_LENGTH -> DomainError.NameTooLong.asStringRes()
                    name.isNotEmpty() && trimmed.isEmpty() -> DomainError.NameEmpty.asStringRes()
                    trimmed.isNotEmpty() && isDuplicateName(trimmed, state.parentId, state.type, state.categoryId) ->
                        DomainError.NameDuplicated.asStringRes()
                    else -> null
                }
            _uiState.update { it.copy(name = name, nameErrorRes = error, generalErrorRes = null) }
        }

        private fun isDuplicateName(
            name: String,
            parentId: Long?,
            type: CategoryType,
            excludeId: Long?,
        ): Boolean {
            val siblings =
                existingCategories.filter {
                    it.type == type && it.parentId == parentId && it.id != excludeId && !it.isArchived
                }
            return categoryNameMatcher.collides(listOf(name.normalizedName()), siblings)
        }

        fun setType(newType: CategoryType) {
            if (_uiState.value.isTypeLocked) return
            viewModelScope.launch {
                _uiState.update { it.copy(type = newType, parentId = null) }
                refreshParentOptions(newType, _uiState.value.categoryId)
            }
        }

        fun setParentId(newParentId: Long?) {
            if (_uiState.value.isParentDisabled) return
            viewModelScope.launch {
                var newColor = _uiState.value.color
                if (newParentId != null && !_uiState.value.isEditMode) {
                    val parent = categoryRepository.getCategory(newParentId)
                    if (parent != null) {
                        newColor = parent.color
                    }
                }
                val txCount =
                    _uiState.value.categoryId?.let { categoryRepository.countTransactions(it) } ?: 0
                val isTypeLocked = newParentId != null || txCount > 0

                _uiState.update {
                    it.copy(
                        parentId = newParentId,
                        color = newColor,
                        isTypeLocked = isTypeLocked,
                        nameErrorRes =
                            if (it.name.isNotBlank()) {
                                if (isDuplicateName(it.name.trim(), newParentId, it.type, it.categoryId)) {
                                    DomainError.NameDuplicated.asStringRes()
                                } else {
                                    null
                                }
                            } else {
                                it.nameErrorRes
                            },
                    )
                }
            }
        }

        fun setColor(color: Int) {
            _uiState.update { it.copy(color = color) }
        }

        fun setIcon(icon: String) {
            _uiState.update { it.copy(icon = icon) }
        }

        fun saveCategory() {
            val state = _uiState.value
            if (!state.isSaveEnabled) return

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, generalErrorRes = null) }
                val trimmed = state.name.trim()
                // Leaving the translated name untouched (in any app language) keeps it translatable.
                val isUntouchedSystemName =
                    state.systemKey != null &&
                        (trimmed == preloadedSystemName || trimmed in systemCategoryNames.namesFor(state.systemKey))
                val nameToSave: String? =
                    if (isUntouchedSystemName) {
                        null
                    } else {
                        trimmed
                    }

                val category =
                    Category(
                        id = state.categoryId ?: 0L,
                        name = nameToSave,
                        systemKey = state.systemKey,
                        type = state.type,
                        parentId = state.parentId,
                        color = state.color,
                        icon = state.icon,
                        isArchived = false,
                    )

                when (val result = saveCategoryUseCase(category)) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _events.send(CategoryEditEvent.NavigateBack(savedCategoryId = result.value))
                    }
                    is DomainResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                generalErrorRes = result.error.asStringRes(),
                            )
                        }
                    }
                }
            }
        }

        fun archiveCategory() {
            val categoryId = _uiState.value.categoryId ?: return
            viewModelScope.launch {
                when (val result = archiveCategoryUseCase(categoryId)) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(deleteDialogState = DeleteDialogState.None) }
                        _events.send(CategoryEditEvent.ShowSnackbar(R.string.category_archived_success))
                        _events.send(CategoryEditEvent.NavigateBack())
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(generalErrorRes = result.error.asStringRes()) }
                    }
                }
            }
        }

        fun onDeleteClick() {
            val categoryId = _uiState.value.categoryId ?: return
            viewModelScope.launch {
                val childrenCount = categoryRepository.countChildren(categoryId)
                if (childrenCount > 0) {
                    _uiState.update { it.copy(deleteDialogState = DeleteDialogState.HasChildren) }
                    return@launch
                }
                val txCount = categoryRepository.countTransactions(categoryId)
                if (txCount > 0) {
                    _uiState.update { it.copy(deleteDialogState = DeleteDialogState.HasTransactions(txCount)) }
                    return@launch
                }
                _uiState.update { it.copy(deleteDialogState = DeleteDialogState.ConfirmSimple) }
            }
        }

        fun dismissDeleteDialog() {
            _uiState.update { it.copy(deleteDialogState = DeleteDialogState.None) }
        }

        fun confirmSimpleDelete() {
            val categoryId = _uiState.value.categoryId ?: return
            viewModelScope.launch {
                when (val result = deleteCategoryUseCase(categoryId)) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(deleteDialogState = DeleteDialogState.None) }
                        _events.send(CategoryEditEvent.NavigateBack())
                    }
                    is DomainResult.Failure -> {
                        when (val err = result.error) {
                            is DomainError.CategoryHasChildren -> {
                                _uiState.update { it.copy(deleteDialogState = DeleteDialogState.HasChildren) }
                            }
                            is DomainError.CategoryHasTransactions -> {
                                _uiState.update { it.copy(deleteDialogState = DeleteDialogState.HasTransactions(err.count)) }
                            }
                            else -> {
                                _uiState.update {
                                    it.copy(
                                        deleteDialogState = DeleteDialogState.None,
                                        generalErrorRes = err.asStringRes(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        fun onReassignOptionSelected() {
            val categoryId = _uiState.value.categoryId ?: return
            val type = _uiState.value.type
            viewModelScope.launch {
                val allCategories =
                    categoryRepository.getAllCategories(type).filter { !it.isArchived && it.id != categoryId }
                val nodes = buildCategoryTree(allCategories)
                _uiState.update {
                    it.copy(
                        deleteDialogState = DeleteDialogState.None,
                        showReassignSheet = true,
                        reassignCategoryNodes = nodes,
                    )
                }
            }
        }

        fun dismissReassignSheet() {
            _uiState.update { it.copy(showReassignSheet = false) }
        }

        fun onReassignAndConfirm(targetCategoryId: Long) {
            val sourceId = _uiState.value.categoryId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(showReassignSheet = false) }
                when (
                    val result =
                        reassignAndDeleteCategoryUseCase(
                            sourceCategoryId = sourceId,
                            targetCategoryId = targetCategoryId,
                        )
                ) {
                    is DomainResult.Success -> {
                        _events.send(CategoryEditEvent.NavigateBack())
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(generalErrorRes = result.error.asStringRes()) }
                    }
                }
            }
        }
    }
