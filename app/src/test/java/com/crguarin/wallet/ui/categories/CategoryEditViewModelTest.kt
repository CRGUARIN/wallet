package com.crguarin.wallet.ui.categories

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeSystemCategoryNames
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.usecase.ArchiveCategoryUseCase
import com.crguarin.wallet.domain.usecase.DeleteCategoryUseCase
import com.crguarin.wallet.domain.usecase.ReassignAndDeleteCategoryUseCase
import com.crguarin.wallet.domain.usecase.SaveCategoryUseCase
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
class CategoryEditViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var systemCategoryNames: FakeSystemCategoryNames
    private lateinit var saveCategoryUseCase: SaveCategoryUseCase
    private lateinit var deleteCategoryUseCase: DeleteCategoryUseCase
    private lateinit var reassignAndDeleteCategoryUseCase: ReassignAndDeleteCategoryUseCase
    private lateinit var archiveCategoryUseCase: ArchiveCategoryUseCase

    private val systemFoodCategory =
        Category(
            id = 10L,
            name = null,
            systemKey = "food",
            type = CategoryType.EXPENSE,
            color = 0xFFEF5350.toInt(),
            icon = "restaurant",
            isArchived = false,
        )

    private val customParentCategory =
        Category(
            id = 20L,
            name = "Vehículo",
            type = CategoryType.EXPENSE,
            color = 0xFF42A5F5.toInt(),
            icon = "car",
            isArchived = false,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = FakeCategoryRepository()
        systemCategoryNames =
            FakeSystemCategoryNames(
                currentNamesMap = mapOf("food" to "Alimentación"),
            )
        saveCategoryUseCase = SaveCategoryUseCase(categoryRepository, systemCategoryNames)
        deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepository)
        reassignAndDeleteCategoryUseCase = ReassignAndDeleteCategoryUseCase(categoryRepository)
        archiveCategoryUseCase = ArchiveCategoryUseCase(categoryRepository)

        categoryRepository.setInitialData(listOf(systemFoodCategory, customParentCategory))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `creation mode initializes defaults and enables save only when name is valid`() =
        runTest(testDispatcher) {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "categoryId" to null,
                        "parentId" to null,
                        "type" to CategoryType.EXPENSE,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            val state = viewModel.uiState.value
            assertThat(state.isEditMode).isFalse()
            assertThat(state.name).isEmpty()
            assertThat(state.type).isEqualTo(CategoryType.EXPENSE)
            assertThat(state.isTypeLocked).isFalse()
            assertThat(state.isSaveEnabled).isFalse()

            viewModel.setName("Nueva")
            assertThat(viewModel.uiState.value.isSaveEnabled).isTrue()
        }

    @Test
    fun `creating subcategory inherits parent color and locks type`() =
        runTest(testDispatcher) {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "categoryId" to null,
                        "parentId" to 20L,
                        "type" to CategoryType.EXPENSE,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            val state = viewModel.uiState.value
            assertThat(state.parentId).isEqualTo(20L)
            assertThat(state.color).isEqualTo(customParentCategory.color)
            assertThat(state.isTypeLocked).isTrue()
        }

    @Test
    fun `edit mode preloads system category with translated name and remembers preloaded name`() =
        runTest(testDispatcher) {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "categoryId" to 10L,
                        "parentId" to null,
                        "type" to CategoryType.EXPENSE,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            val state = viewModel.uiState.value
            assertThat(state.isEditMode).isTrue()
            assertThat(state.name).isEqualTo("Alimentación")
            assertThat(state.systemKey).isEqualTo("food")
            assertThat(viewModel.preloadedSystemName).isEqualTo("Alimentación")
        }

    @Test
    fun `saving system category without changing name saves name as null`() =
        runTest(testDispatcher) {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "categoryId" to 10L,
                        "parentId" to null,
                        "type" to CategoryType.EXPENSE,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            viewModel.events.test {
                viewModel.saveCategory()
                val event = awaitItem()
                assertThat(event).isEqualTo(CategoryEditEvent.NavigateBack)
            }

            val saved = categoryRepository.getCategory(10L)
            assertThat(saved?.name).isNull()
            assertThat(saved?.systemKey).isEqualTo("food")
        }

    @Test
    fun `saving system category with changed name saves updated name`() =
        runTest(testDispatcher) {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "categoryId" to 10L,
                        "parentId" to null,
                        "type" to CategoryType.EXPENSE,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            viewModel.setName("Comida rápida")
            viewModel.events.test {
                viewModel.saveCategory()
                val event = awaitItem()
                assertThat(event).isEqualTo(CategoryEditEvent.NavigateBack)
            }

            val saved = categoryRepository.getCategory(10L)
            assertThat(saved?.name).isEqualTo("Comida rápida")
            assertThat(saved?.systemKey).isEqualTo("food")
        }

    @Test
    fun `type is locked when category has transactions`() =
        runTest(testDispatcher) {
            categoryRepository.setTransactionCount(20L, 5)

            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "categoryId" to 20L,
                        "parentId" to null,
                        "type" to CategoryType.EXPENSE,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            val state = viewModel.uiState.value
            assertThat(state.isTypeLocked).isTrue()
        }

    @Test
    fun `parent selector is disabled when category has subcategories`() =
        runTest(testDispatcher) {
            val child =
                Category(
                    id = 21L,
                    name = "Taller",
                    type = CategoryType.EXPENSE,
                    parentId = 20L,
                    color = 1,
                    icon = "build",
                )
            categoryRepository.saveCategory(child)

            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "categoryId" to 20L,
                        "parentId" to null,
                        "type" to CategoryType.EXPENSE,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            val state = viewModel.uiState.value
            assertThat(state.isParentDisabled).isTrue()
        }

    @Test
    fun `delete path 1 - category without transactions or children prompts simple confirm and deletes`() =
        runTest(testDispatcher) {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "categoryId" to 20L,
                        "parentId" to null,
                        "type" to CategoryType.EXPENSE,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            viewModel.onDeleteClick()
            assertThat(viewModel.uiState.value.deleteDialogState)
                .isEqualTo(DeleteDialogState.ConfirmSimple)

            viewModel.events.test {
                viewModel.confirmSimpleDelete()
                val event = awaitItem()
                assertThat(event).isEqualTo(CategoryEditEvent.NavigateBack)
            }

            assertThat(categoryRepository.getCategory(20L)).isNull()
        }

    @Test
    fun `delete path 2 - category with children prompts HasChildren dialog`() =
        runTest(testDispatcher) {
            val child =
                Category(
                    id = 21L,
                    name = "Taller",
                    type = CategoryType.EXPENSE,
                    parentId = 20L,
                    color = 1,
                    icon = "build",
                )
            categoryRepository.saveCategory(child)

            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "categoryId" to 20L,
                        "parentId" to null,
                        "type" to CategoryType.EXPENSE,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            viewModel.onDeleteClick()
            assertThat(viewModel.uiState.value.deleteDialogState)
                .isEqualTo(DeleteDialogState.HasChildren)

            // Choosing archive archives parent and children and navigates back
            viewModel.events.test {
                viewModel.archiveCategory()
                val snackbarEvent = awaitItem()
                val navEvent = awaitItem()
                assertThat(snackbarEvent).isInstanceOf(CategoryEditEvent.ShowSnackbar::class.java)
                assertThat(navEvent).isEqualTo(CategoryEditEvent.NavigateBack)
            }
            assertThat(categoryRepository.getCategory(20L)?.isArchived).isTrue()
            assertThat(categoryRepository.getCategory(21L)?.isArchived).isTrue()
        }

    @Test
    fun `delete path 3 - category with transactions prompts HasTransactions dialog and supports reassign and delete`() =
        runTest(testDispatcher) {
            categoryRepository.setTransactionCount(20L, 8)

            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "categoryId" to 20L,
                        "parentId" to null,
                        "type" to CategoryType.EXPENSE,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            viewModel.onDeleteClick()
            val dialogState = viewModel.uiState.value.deleteDialogState
            assertThat(dialogState).isInstanceOf(DeleteDialogState.HasTransactions::class.java)
            assertThat((dialogState as DeleteDialogState.HasTransactions).count).isEqualTo(8)

            // Select "Reasignar y eliminar" -> opens sheet
            viewModel.onReassignOptionSelected()
            val stateAfterOption = viewModel.uiState.value
            assertThat(stateAfterOption.showReassignSheet).isTrue()
            assertThat(stateAfterOption.reassignCategoryNodes).hasSize(1)
            assertThat(
                stateAfterOption.reassignCategoryNodes
                    .first()
                    .category.id,
            ).isEqualTo(10L)

            // Pick target category (10L)
            viewModel.events.test {
                viewModel.onReassignAndConfirm(10L)
                val event = awaitItem()
                assertThat(event).isEqualTo(CategoryEditEvent.NavigateBack)
            }

            assertThat(categoryRepository.getCategory(20L)).isNull()
            assertThat(categoryRepository.countTransactions(10L)).isEqualTo(8)
        }

    private fun createViewModel(savedStateHandle: SavedStateHandle): CategoryEditViewModel =
        CategoryEditViewModel(
            savedStateHandle = savedStateHandle,
            categoryRepository = categoryRepository,
            saveCategoryUseCase = saveCategoryUseCase,
            deleteCategoryUseCase = deleteCategoryUseCase,
            reassignAndDeleteCategoryUseCase = reassignAndDeleteCategoryUseCase,
            archiveCategoryUseCase = archiveCategoryUseCase,
            systemCategoryNames = systemCategoryNames,
        )
}
