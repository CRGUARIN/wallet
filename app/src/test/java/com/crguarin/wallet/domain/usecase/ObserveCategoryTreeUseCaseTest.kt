package com.crguarin.wallet.domain.usecase

import app.cash.turbine.test
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ObserveCategoryTreeUseCaseTest {
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var observeCategoryTreeUseCase: ObserveCategoryTreeUseCase

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        observeCategoryTreeUseCase = ObserveCategoryTreeUseCase(categoryRepository)
    }

    @Test
    fun `builds category tree correctly for specified type`() =
        runTest {
            val rootExpense =
                Category(
                    id = 1L,
                    name = "Alimentación",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 1,
                    icon = "restaurant",
                    sortOrder = 0,
                )
            val sub1 =
                Category(
                    id = 2L,
                    name = "Mercado",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "restaurant",
                    sortOrder = 0,
                )
            val sub2 =
                Category(
                    id = 3L,
                    name = "Restaurantes",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "restaurant",
                    sortOrder = 1,
                )
            val rootIncome =
                Category(
                    id = 4L,
                    name = "Salario",
                    type = CategoryType.INCOME,
                    parentId = null,
                    color = 2,
                    icon = "payments",
                    sortOrder = 0,
                )
            categoryRepository.setInitialData(listOf(rootExpense, sub1, sub2, rootIncome))

            observeCategoryTreeUseCase(CategoryType.EXPENSE).test {
                val tree = awaitItem()
                assertThat(tree).hasSize(1)
                val node = tree.first()
                assertThat(node.category.id).isEqualTo(1L)
                assertThat(node.children).hasSize(2)
                assertThat(node.children[0].category.id).isEqualTo(2L)
                assertThat(node.children[1].category.id).isEqualTo(3L)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
