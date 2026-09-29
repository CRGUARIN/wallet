package com.crguarin.wallet.ui.categories

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.crguarin.wallet.R
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.usecase.ObserveCategoryTreeUseCase
import com.crguarin.wallet.domain.usecase.UnarchiveCategoryUseCase
import com.crguarin.wallet.ui.navigation.Categories
import com.crguarin.wallet.ui.util.asStringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val isLoading: Boolean = false,
    val selectedType: CategoryType = CategoryType.EXPENSE,
    val activeTree: List<CategoryNode> = emptyList(),
    val archivedCategories: List<Category> = emptyList(),
    val isArchivedExpanded: Boolean = false,
)

sealed interface CategoriesEvent {
    data class ShowSnackbar(
        @StringRes val messageRes: Int,
    ) : CategoriesEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val observeCategoryTreeUseCase: ObserveCategoryTreeUseCase,
        private val categoryRepository: CategoryRepository,
        private val unarchiveCategoryUseCase: UnarchiveCategoryUseCase,
    ) : ViewModel() {
        private val initialType: CategoryType = savedStateHandle.toRoute<Categories>().type

        val selectedType: StateFlow<CategoryType> =
            savedStateHandle.getStateFlow(KEY_SELECTED_TYPE, initialType)

        val isArchivedExpanded: StateFlow<Boolean> =
            savedStateHandle.getStateFlow(KEY_IS_ARCHIVED_EXPANDED, false)

        private val _events = Channel<CategoriesEvent>(Channel.BUFFERED)
        val events: Flow<CategoriesEvent> = _events.receiveAsFlow()

        val uiState: StateFlow<CategoriesUiState> =
            selectedType
                .flatMapLatest { type ->
                    combine(
                        observeCategoryTreeUseCase(type, includeArchived = false),
                        categoryRepository.observeCategories(type, includeArchived = true).map { list ->
                            list.filter { it.isArchived }
                        },
                        isArchivedExpanded,
                    ) { activeTree, archivedList, expanded ->
                        CategoriesUiState(
                            isLoading = false,
                            selectedType = type,
                            activeTree = activeTree,
                            archivedCategories = archivedList,
                            isArchivedExpanded = expanded,
                        )
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = CategoriesUiState(isLoading = true, selectedType = initialType),
                )

        fun selectType(type: CategoryType) {
            savedStateHandle[KEY_SELECTED_TYPE] = type
        }

        fun toggleArchivedExpanded() {
            val current = isArchivedExpanded.value
            savedStateHandle[KEY_IS_ARCHIVED_EXPANDED] = !current
        }

        fun unarchiveCategory(categoryId: Long) {
            viewModelScope.launch {
                when (val result = unarchiveCategoryUseCase(categoryId)) {
                    is DomainResult.Success -> {
                        _events.send(CategoriesEvent.ShowSnackbar(R.string.category_unarchived_success))
                    }
                    is DomainResult.Failure -> {
                        _events.send(CategoriesEvent.ShowSnackbar(result.error.asStringRes()))
                    }
                }
            }
        }

        companion object {
            private const val KEY_SELECTED_TYPE = "selected_type"
            private const val KEY_IS_ARCHIVED_EXPANDED = "is_archived_expanded"
        }
    }
