package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DeleteCategoryUseCaseTest {
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var deleteCategoryUseCase: DeleteCategoryUseCase

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepository)
    }

    @Test
    fun `fails when category not found`() =
        runTest {
            val result = deleteCategoryUseCase(999L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `fails when category has children`() =
        runTest {
            val parent =
                Category(
                    id = 1L,
                    name = "Padre",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "home",
                )
            val child =
                Category(
                    id = 2L,
                    name = "Hijo",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "home",
                )
            categoryRepository.setInitialData(listOf(parent, child))

            val result = deleteCategoryUseCase(1L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.CategoryHasChildren)
            assertThat(categoryRepository.getCategory(1L)).isNotNull()
        }

    @Test
    fun `fails when category has transactions`() =
        runTest {
            val cat =
                Category(
                    id = 1L,
                    name = "Restaurantes",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                )
            categoryRepository.setInitialData(listOf(cat), txCounts = mapOf(1L to 3))

            val result = deleteCategoryUseCase(1L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.CategoryHasTransactions(3))
            assertThat(categoryRepository.getCategory(1L)).isNotNull()
        }

    @Test
    fun `deletes category when it has no children and no transactions`() =
        runTest {
            val cat =
                Category(
                    id = 1L,
                    name = "Restaurantes",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                )
            categoryRepository.setInitialData(listOf(cat), txCounts = mapOf(1L to 0))

            val result = deleteCategoryUseCase(1L)
            assertThat(result.isSuccess).isTrue()
            assertThat(categoryRepository.getCategory(1L)).isNull()
        }
}
