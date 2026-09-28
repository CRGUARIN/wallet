package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ReassignAndDeleteCategoryUseCaseTest {
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var reassignAndDeleteCategoryUseCase: ReassignAndDeleteCategoryUseCase

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        reassignAndDeleteCategoryUseCase = ReassignAndDeleteCategoryUseCase(categoryRepository)
    }

    @Test
    fun `fails when source and target are the same category`() =
        runTest {
            val result = reassignAndDeleteCategoryUseCase(1L, 1L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.SameCategoryReassignment)
        }

    @Test
    fun `fails when source does not exist`() =
        runTest {
            val target =
                Category(
                    id = 2L,
                    name = "Destino",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                )
            categoryRepository.setInitialData(listOf(target))

            val result = reassignAndDeleteCategoryUseCase(1L, 2L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `fails when target does not exist`() =
        runTest {
            val source =
                Category(
                    id = 1L,
                    name = "Origen",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                )
            categoryRepository.setInitialData(listOf(source))

            val result = reassignAndDeleteCategoryUseCase(1L, 2L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `fails when source and target have different types`() =
        runTest {
            val sourceExpense =
                Category(
                    id = 1L,
                    name = "Gasto",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                )
            val targetIncome =
                Category(
                    id = 2L,
                    name = "Ingreso",
                    type = CategoryType.INCOME,
                    color = 2,
                    icon = "payments",
                )
            categoryRepository.setInitialData(listOf(sourceExpense, targetIncome))

            val result = reassignAndDeleteCategoryUseCase(1L, 2L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.CategoryTypeMismatch)
        }

    @Test
    fun `fails when source category has children`() =
        runTest {
            val sourceParent =
                Category(
                    id = 1L,
                    name = "Padre",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "home",
                )
            val child =
                Category(
                    id = 3L,
                    name = "Hijo",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "home",
                )
            val target =
                Category(
                    id = 2L,
                    name = "Otro",
                    type = CategoryType.EXPENSE,
                    color = 2,
                    icon = "home",
                )
            categoryRepository.setInitialData(listOf(sourceParent, child, target))

            val result = reassignAndDeleteCategoryUseCase(1L, 2L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.CategoryHasChildren)
        }

    @Test
    fun `reassigns transactions and deletes source category successfully`() =
        runTest {
            val source =
                Category(
                    id = 1L,
                    name = "Origen",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                )
            val target =
                Category(
                    id = 2L,
                    name = "Destino",
                    type = CategoryType.EXPENSE,
                    color = 2,
                    icon = "restaurant",
                )
            categoryRepository.setInitialData(listOf(source, target), txCounts = mapOf(1L to 4, 2L to 2))

            val result = reassignAndDeleteCategoryUseCase(1L, 2L)
            assertThat(result.isSuccess).isTrue()

            assertThat(categoryRepository.getCategory(1L)).isNull()
            assertThat(categoryRepository.getCategory(2L)).isNotNull()
            assertThat(categoryRepository.countTransactions(2L)).isEqualTo(6)
        }
}
