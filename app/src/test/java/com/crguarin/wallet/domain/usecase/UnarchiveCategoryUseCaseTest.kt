package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeSystemCategoryNames
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNameMatcher
import com.crguarin.wallet.domain.model.CategoryType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UnarchiveCategoryUseCaseTest {
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var systemCategoryNames: FakeSystemCategoryNames
    private lateinit var unarchiveCategoryUseCase: UnarchiveCategoryUseCase

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        systemCategoryNames = FakeSystemCategoryNames()
        unarchiveCategoryUseCase = UnarchiveCategoryUseCase(categoryRepository, CategoryNameMatcher(systemCategoryNames))
    }

    @Test
    fun `unarchive root succeeds and leaves subcategories as they were`() =
        runTest {
            val root =
                Category(
                    id = 1L,
                    name = "Vehículo",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "car",
                    isArchived = true,
                )
            val sub1 =
                Category(
                    id = 2L,
                    name = "Taller",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "build",
                    isArchived = true,
                )
            categoryRepository.setInitialData(listOf(root, sub1))

            val result = unarchiveCategoryUseCase(1L)

            assertThat(result.isSuccess).isTrue()
            val updatedRoot = categoryRepository.getCategory(1L)
            val updatedSub = categoryRepository.getCategory(2L)
            assertThat(updatedRoot?.isArchived).isFalse()
            assertThat(updatedSub?.isArchived).isTrue()
        }

    @Test
    fun `unarchive subcategory with active parent unarchives only the subcategory`() =
        runTest {
            val root =
                Category(
                    id = 1L,
                    name = "Vehículo",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "car",
                    isArchived = false,
                )
            val sub =
                Category(
                    id = 2L,
                    name = "Taller",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "build",
                    isArchived = true,
                )
            categoryRepository.setInitialData(listOf(root, sub))

            val result = unarchiveCategoryUseCase(2L)

            assertThat(result.isSuccess).isTrue()
            assertThat(categoryRepository.getCategory(2L)?.isArchived).isFalse()
            assertThat(categoryRepository.getCategory(1L)?.isArchived).isFalse()
        }

    @Test
    fun `unarchive subcategory with archived parent unarchives both subcategory and parent`() =
        runTest {
            val root =
                Category(
                    id = 1L,
                    name = "Vehículo",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "car",
                    isArchived = true,
                )
            val sub =
                Category(
                    id = 2L,
                    name = "Taller",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "build",
                    isArchived = true,
                )
            categoryRepository.setInitialData(listOf(root, sub))

            val result = unarchiveCategoryUseCase(2L)

            assertThat(result.isSuccess).isTrue()
            assertThat(categoryRepository.getCategory(1L)?.isArchived).isFalse()
            assertThat(categoryRepository.getCategory(2L)?.isArchived).isFalse()
        }

    @Test
    fun `unarchive root fails when an active root has duplicate name`() =
        runTest {
            val activeRoot =
                Category(
                    id = 1L,
                    name = "Vehículo",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "car",
                    isArchived = false,
                )
            val archivedRoot =
                Category(
                    id = 2L,
                    name = "  vehículo  ",
                    type = CategoryType.EXPENSE,
                    color = 2,
                    icon = "car",
                    isArchived = true,
                )
            categoryRepository.setInitialData(listOf(activeRoot, archivedRoot))

            val result = unarchiveCategoryUseCase(2L)

            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameDuplicated)
            assertThat(categoryRepository.getCategory(2L)?.isArchived).isTrue()
        }

    @Test
    fun `unarchive root fails when an active system root matches translated name`() =
        runTest {
            val activeSystemRoot =
                Category(
                    id = 1L,
                    name = null,
                    systemKey = "food",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                    isArchived = false,
                )
            val archivedRoot =
                Category(
                    id = 2L,
                    name = "Alimentación",
                    type = CategoryType.EXPENSE,
                    color = 2,
                    icon = "restaurant",
                    isArchived = true,
                )
            categoryRepository.setInitialData(listOf(activeSystemRoot, archivedRoot))

            val result = unarchiveCategoryUseCase(2L)

            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameDuplicated)
        }

    @Test
    fun `unarchive subcategory fails when an active sibling under same parent has duplicate name`() =
        runTest {
            val root =
                Category(
                    id = 1L,
                    name = "Gastos",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "wallet",
                    isArchived = false,
                )
            val activeSub =
                Category(
                    id = 2L,
                    name = "Gasolina",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "fuel",
                    isArchived = false,
                )
            val archivedSub =
                Category(
                    id = 3L,
                    name = "gasolina",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "fuel",
                    isArchived = true,
                )
            categoryRepository.setInitialData(listOf(root, activeSub, archivedSub))

            val result = unarchiveCategoryUseCase(3L)

            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameDuplicated)
        }

    @Test
    fun `unarchive subcategory with archived parent fails when parent collides with an active root`() =
        runTest {
            val activeRoot =
                Category(
                    id = 1L,
                    name = "Hogar",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "home",
                    isArchived = false,
                )
            val archivedRoot =
                Category(
                    id = 2L,
                    name = "hogar",
                    type = CategoryType.EXPENSE,
                    color = 2,
                    icon = "home",
                    isArchived = true,
                )
            val archivedSub =
                Category(
                    id = 3L,
                    name = "Muebles",
                    type = CategoryType.EXPENSE,
                    parentId = 2L,
                    color = 2,
                    icon = "chair",
                    isArchived = true,
                )
            categoryRepository.setInitialData(listOf(activeRoot, archivedRoot, archivedSub))

            val result = unarchiveCategoryUseCase(3L)

            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameDuplicated)
            assertThat(categoryRepository.getCategory(2L)?.isArchived).isTrue()
            assertThat(categoryRepository.getCategory(3L)?.isArchived).isTrue()
        }

    @Test
    fun `unarchive non-existent category returns NotFound`() =
        runTest {
            val result = unarchiveCategoryUseCase(999L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `unarchive already active category returns Success without changes`() =
        runTest {
            val root =
                Category(
                    id = 1L,
                    name = "Vehículo",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "car",
                    isArchived = false,
                )
            categoryRepository.setInitialData(listOf(root))

            val result = unarchiveCategoryUseCase(1L)
            assertThat(result.isSuccess).isTrue()
        }
}
