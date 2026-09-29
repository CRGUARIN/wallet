package com.crguarin.wallet.ui.categories

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeSystemCategoryNames
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNameMatcher
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.usecase.ObserveCategoryTreeUseCase
import com.crguarin.wallet.domain.usecase.UnarchiveCategoryUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var systemCategoryNames: FakeSystemCategoryNames
    private lateinit var observeCategoryTreeUseCase: ObserveCategoryTreeUseCase
    private lateinit var unarchiveCategoryUseCase: UnarchiveCategoryUseCase

    private val expenseRoot =
        Category(
            id = 1L,
            name = "Alimentación",
            type = CategoryType.EXPENSE,
            color = 1,
            icon = "restaurant",
            isArchived = false,
        )
    private val expenseSub =
        Category(
            id = 2L,
            name = "Mercado",
            type = CategoryType.EXPENSE,
            parentId = 1L,
            color = 1,
            icon = "shopping_cart",
            isArchived = false,
        )
    private val expenseArchived =
        Category(
            id = 3L,
            name = "Gasto Viejo",
            type = CategoryType.EXPENSE,
            color = 2,
            icon = "delete",
            isArchived = true,
        )
    private val incomeRoot =
        Category(
            id = 4L,
            name = "Salario",
            type = CategoryType.INCOME,
            color = 3,
            icon = "work",
            isArchived = false,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = FakeCategoryRepository()
        systemCategoryNames = FakeSystemCategoryNames()
        observeCategoryTreeUseCase = ObserveCategoryTreeUseCase(categoryRepository)
        unarchiveCategoryUseCase = UnarchiveCategoryUseCase(categoryRepository, CategoryNameMatcher(systemCategoryNames))

        categoryRepository.setInitialData(
            listOf(expenseRoot, expenseSub, expenseArchived, incomeRoot),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial tab comes from route args and displays active tree and archived items`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("type" to CategoryType.EXPENSE))
            val viewModel =
                CategoriesViewModel(
                    savedStateHandle = savedStateHandle,
                    observeCategoryTreeUseCase = observeCategoryTreeUseCase,
                    categoryRepository = categoryRepository,
                    unarchiveCategoryUseCase = unarchiveCategoryUseCase,
                )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.selectedType).isEqualTo(CategoryType.EXPENSE)
                assertThat(state.activeTree).hasSize(1)
                assertThat(
                    state.activeTree
                        .first()
                        .category.id,
                ).isEqualTo(1L)
                assertThat(state.activeTree.first().children).hasSize(1)
                assertThat(
                    state.activeTree
                        .first()
                        .children
                        .first()
                        .category.id,
                ).isEqualTo(2L)
                assertThat(state.archivedCategories).hasSize(1)
                assertThat(state.archivedCategories.first().id).isEqualTo(3L)
            }
        }

    @Test
    fun `switching tab updates selectedType and filters tree and archived list`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("type" to CategoryType.EXPENSE))
            val viewModel =
                CategoriesViewModel(
                    savedStateHandle = savedStateHandle,
                    observeCategoryTreeUseCase = observeCategoryTreeUseCase,
                    categoryRepository = categoryRepository,
                    unarchiveCategoryUseCase = unarchiveCategoryUseCase,
                )

            viewModel.uiState.test {
                val state1 = awaitItem()
                assertThat(state1.selectedType).isEqualTo(CategoryType.EXPENSE)

                viewModel.selectType(CategoryType.INCOME)
                val state2 = awaitItem()
                assertThat(state2.selectedType).isEqualTo(CategoryType.INCOME)
                assertThat(state2.activeTree).hasSize(1)
                assertThat(
                    state2.activeTree
                        .first()
                        .category.id,
                ).isEqualTo(4L)
                assertThat(state2.archivedCategories).isEmpty()
            }
        }

    @Test
    fun `toggleArchivedExpanded toggles isArchivedExpanded`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("type" to CategoryType.EXPENSE))
            val viewModel =
                CategoriesViewModel(
                    savedStateHandle = savedStateHandle,
                    observeCategoryTreeUseCase = observeCategoryTreeUseCase,
                    categoryRepository = categoryRepository,
                    unarchiveCategoryUseCase = unarchiveCategoryUseCase,
                )

            viewModel.uiState.test {
                val state1 = awaitItem()
                assertThat(state1.isArchivedExpanded).isFalse()

                viewModel.toggleArchivedExpanded()
                val state2 = awaitItem()
                assertThat(state2.isArchivedExpanded).isTrue()

                viewModel.toggleArchivedExpanded()
                val state3 = awaitItem()
                assertThat(state3.isArchivedExpanded).isFalse()
            }
        }

    @Test
    fun `unarchiveCategory success emits snackbar event and updates lists`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("type" to CategoryType.EXPENSE))
            val viewModel =
                CategoriesViewModel(
                    savedStateHandle = savedStateHandle,
                    observeCategoryTreeUseCase = observeCategoryTreeUseCase,
                    categoryRepository = categoryRepository,
                    unarchiveCategoryUseCase = unarchiveCategoryUseCase,
                )

            viewModel.events.test {
                viewModel.unarchiveCategory(3L)
                val event = awaitItem()
                assertThat(event).isInstanceOf(CategoriesEvent.ShowSnackbar::class.java)
                assertThat((event as CategoriesEvent.ShowSnackbar).messageRes)
                    .isEqualTo(R.string.category_unarchived_success)
            }

            assertThat(categoryRepository.getCategory(3L)?.isArchived).isFalse()
        }

    @Test
    fun `unarchiveCategory failure on duplicate name emits error snackbar event`() =
        runTest(testDispatcher) {
            // Add archived category with duplicate name of active root
            val duplicateArchived =
                Category(
                    id = 5L,
                    name = "Alimentación",
                    type = CategoryType.EXPENSE,
                    color = 2,
                    icon = "restaurant",
                    isArchived = true,
                )
            categoryRepository.saveCategory(duplicateArchived)

            val savedStateHandle = SavedStateHandle(mapOf("type" to CategoryType.EXPENSE))
            val viewModel =
                CategoriesViewModel(
                    savedStateHandle = savedStateHandle,
                    observeCategoryTreeUseCase = observeCategoryTreeUseCase,
                    categoryRepository = categoryRepository,
                    unarchiveCategoryUseCase = unarchiveCategoryUseCase,
                )

            viewModel.events.test {
                viewModel.unarchiveCategory(5L)
                val event = awaitItem()
                assertThat(event).isInstanceOf(CategoriesEvent.ShowSnackbar::class.java)
                assertThat((event as CategoriesEvent.ShowSnackbar).messageRes)
                    .isEqualTo(R.string.error_name_duplicated)
            }
        }
}
