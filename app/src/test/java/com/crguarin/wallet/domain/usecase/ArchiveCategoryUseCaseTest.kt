package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ArchiveCategoryUseCaseTest {
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var archiveCategoryUseCase: ArchiveCategoryUseCase

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        archiveCategoryUseCase = ArchiveCategoryUseCase(categoryRepository)
    }

    @Test
    fun `fails when category not found`() =
        runTest {
            val result = archiveCategoryUseCase(999L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `archives category and its subcategories`() =
        runTest {
            val parent =
                Category(
                    id = 1L,
                    name = "Vivienda",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "home",
                    isArchived = false,
                )
            val sub1 =
                Category(
                    id = 2L,
                    name = "Arriendo",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "home",
                    isArchived = false,
                )
            val sub2 =
                Category(
                    id = 3L,
                    name = "Servicios",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "power",
                    isArchived = false,
                )
            val unrelated =
                Category(
                    id = 4L,
                    name = "Salud",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 2,
                    icon = "medical_services",
                    isArchived = false,
                )
            categoryRepository.setInitialData(listOf(parent, sub1, sub2, unrelated))

            val result = archiveCategoryUseCase(1L)
            assertThat(result.isSuccess).isTrue()

            assertThat(categoryRepository.getCategory(1L)?.isArchived).isTrue()
            assertThat(categoryRepository.getCategory(2L)?.isArchived).isTrue()
            assertThat(categoryRepository.getCategory(3L)?.isArchived).isTrue()
            assertThat(categoryRepository.getCategory(4L)?.isArchived).isFalse()
        }
}
