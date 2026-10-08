package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeSystemCategoryNames
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNameMatcher
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.card.CardSystemCategories
import com.crguarin.wallet.domain.model.recurring.testPayment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SaveCategoryUseCaseTest {
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var systemCategoryNames: FakeSystemCategoryNames
    private lateinit var saveCategoryUseCase: SaveCategoryUseCase
    private val red = 0xFFEF5350.toInt()
    private val blue = 0xFF42A5F5.toInt()
    private val green = 0xFF66BB6A.toInt()

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        recurringRepository = FakeRecurringRepository()
        systemCategoryNames = FakeSystemCategoryNames()
        saveCategoryUseCase =
            SaveCategoryUseCase(
                categoryRepository,
                recurringRepository,
                CategoryNameMatcher(systemCategoryNames),
                FakeTransactionRunner(),
            )
    }

    @Test
    fun `fails when name is empty`() =
        runTest {
            val category =
                Category(
                    name = "   ",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                )
            val result = saveCategoryUseCase(category)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameEmpty)
        }

    @Test
    fun `fails when name is too long`() =
        runTest {
            val category =
                Category(
                    name = "C".repeat(41),
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                )
            val result = saveCategoryUseCase(category)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameTooLong)
        }

    @Test
    fun `fails when parent does not exist`() =
        runTest {
            val category =
                Category(
                    name = "Sub",
                    type = CategoryType.EXPENSE,
                    parentId = 999L,
                    color = 1,
                    icon = "restaurant",
                )
            val result = saveCategoryUseCase(category)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `fails when parent is already a subcategory`() =
        runTest {
            val root =
                Category(
                    id = 1L,
                    name = "Alimentación",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 1,
                    icon = "restaurant",
                )
            val sub =
                Category(
                    id = 2L,
                    name = "Restaurantes",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "restaurant",
                )
            categoryRepository.setInitialData(listOf(root, sub))

            val subSub =
                Category(
                    name = "Comida Rápida",
                    type = CategoryType.EXPENSE,
                    parentId = 2L,
                    color = 1,
                    icon = "restaurant",
                )
            val result = saveCategoryUseCase(subSub)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.MaxDepthExceeded)
        }

    @Test
    fun `fails when child type does not match parent type`() =
        runTest {
            val rootExpense =
                Category(
                    id = 1L,
                    name = "Alimentación",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 1,
                    icon = "restaurant",
                )
            categoryRepository.setInitialData(listOf(rootExpense))

            val childIncome =
                Category(
                    name = "Devolución Comida",
                    type = CategoryType.INCOME,
                    parentId = 1L,
                    color = 1,
                    icon = "restaurant",
                )
            val result = saveCategoryUseCase(childIncome)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.CategoryTypeMismatch)
        }

    @Test
    fun `fails when converting a category with children into a subcategory`() =
        runTest {
            val root1 =
                Category(
                    id = 1L,
                    name = "Padre 1",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 1,
                    icon = "home",
                )
            val root2 =
                Category(
                    id = 2L,
                    name = "Padre 2",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 1,
                    icon = "home",
                )
            val childOf1 =
                Category(
                    id = 3L,
                    name = "Hijo de 1",
                    type = CategoryType.EXPENSE,
                    parentId = 1L,
                    color = 1,
                    icon = "home",
                )
            categoryRepository.setInitialData(listOf(root1, root2, childOf1))

            // Try to make root1 a child of root2
            val updateRoot1 = root1.copy(parentId = 2L)
            val result = saveCategoryUseCase(updateRoot1)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.MaxDepthExceeded)
        }

    @Test
    fun `fails when changing type of category with transactions`() =
        runTest {
            val category =
                Category(
                    id = 1L,
                    name = "Compras",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 1,
                    icon = "shopping_bag",
                )
            categoryRepository.setInitialData(listOf(category), txCounts = mapOf(1L to 5))

            val changeType = category.copy(type = CategoryType.INCOME)
            val result = saveCategoryUseCase(changeType)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.CategoryHasTransactions(5))
        }

    @Test
    fun `fails when changing type of category with recurring payments`() =
        runTest {
            val category =
                Category(
                    id = 1L,
                    name = "Suscripciones",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "subscriptions",
                )
            categoryRepository.setInitialData(listOf(category))
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, categoryId = 1L)))

            val result = saveCategoryUseCase(category.copy(type = CategoryType.INCOME))
            assertThat(result.errorOrNull()).isEqualTo(DomainError.CategoryHasRecurring)
        }

    @Test
    fun `fails when changing type of a card system category`() =
        runTest {
            val interestFees =
                Category(
                    id = 1L,
                    systemKey = CardSystemCategories.INTEREST_FEES_KEY,
                    type = CardSystemCategories.INTEREST_FEES_TYPE,
                    color = CardSystemCategories.INTEREST_FEES_COLOR,
                    icon = CardSystemCategories.INTEREST_FEES_ICON,
                )
            categoryRepository.setInitialData(listOf(interestFees))

            val result = saveCategoryUseCase(interestFees.copy(type = CategoryType.INCOME))
            assertThat(result.errorOrNull()).isEqualTo(DomainError.CategoryTypeMismatch)
        }

    @Test
    fun `keeping the type of a card system category still saves`() =
        runTest {
            val interestFees =
                Category(
                    id = 1L,
                    systemKey = CardSystemCategories.INTEREST_FEES_KEY,
                    type = CardSystemCategories.INTEREST_FEES_TYPE,
                    color = CardSystemCategories.INTEREST_FEES_COLOR,
                    icon = CardSystemCategories.INTEREST_FEES_ICON,
                )
            categoryRepository.setInitialData(listOf(interestFees))

            val result = saveCategoryUseCase(interestFees.copy(color = 2))
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `fails when duplicate name among siblings`() =
        runTest {
            val sibling1 =
                Category(
                    id = 1L,
                    name = "Alimentación",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 1,
                    icon = "restaurant",
                )
            categoryRepository.setInitialData(listOf(sibling1))

            val candidate =
                Category(
                    name = "  alimentación  ",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 2,
                    icon = "restaurant",
                )
            val result = saveCategoryUseCase(candidate)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameDuplicated)
        }

    @Test
    fun `fails when name matches system category translation in ES or EN`() =
        runTest {
            val systemCat =
                Category(
                    id = 1L,
                    name = null,
                    systemKey = "food",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 1,
                    icon = "restaurant",
                )
            categoryRepository.setInitialData(listOf(systemCat))

            // Sibling attempts to use "Food" (English translation of food)
            val enDuplicate =
                Category(
                    name = "Food",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 2,
                    icon = "restaurant",
                )
            val resultEn = saveCategoryUseCase(enDuplicate)
            assertThat(resultEn.errorOrNull()).isEqualTo(DomainError.NameDuplicated)

            // Sibling attempts to use "Alimentación" (Spanish translation of food)
            val esDuplicate =
                Category(
                    name = "alimentación",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 2,
                    icon = "restaurant",
                )
            val resultEs = saveCategoryUseCase(esDuplicate)
            assertThat(resultEs.errorOrNull()).isEqualTo(DomainError.NameDuplicated)
        }

    @Test
    fun `allows same name under different parent or different type`() =
        runTest {
            val expenseCat =
                Category(
                    id = 1L,
                    name = "General",
                    type = CategoryType.EXPENSE,
                    parentId = null,
                    color = 1,
                    icon = "more_horiz",
                )
            categoryRepository.setInitialData(listOf(expenseCat))

            val incomeCandidate =
                Category(
                    name = "General",
                    type = CategoryType.INCOME,
                    parentId = null,
                    color = 1,
                    icon = "more_horiz",
                )
            val result = saveCategoryUseCase(incomeCandidate)
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `fails when a category is its own parent`() =
        runTest {
            val id =
                saveCategoryUseCase(
                    Category(name = "Comida", type = CategoryType.EXPENSE, color = 1, icon = "restaurant"),
                ).getOrNull()!!
            val stored = categoryRepository.getCategory(id)!!

            val result = saveCategoryUseCase(stored.copy(parentId = id))
            assertThat(result.errorOrNull()).isEqualTo(DomainError.MaxDepthExceeded)
        }

    @Test
    fun `new category is appended after its siblings`() =
        runTest {
            val first = Category(id = 1L, name = "A", type = CategoryType.EXPENSE, color = 1, icon = "home", sortOrder = 4)
            categoryRepository.setInitialData(listOf(first))

            val id =
                saveCategoryUseCase(
                    Category(name = "B", type = CategoryType.EXPENSE, color = 2, icon = "home", sortOrder = 0, isArchived = true),
                ).getOrNull()!!

            val saved = categoryRepository.getCategory(id)!!
            assertThat(saved.sortOrder).isEqualTo(5)
            assertThat(saved.isArchived).isFalse()
        }

    @Test
    fun `editing keeps sortOrder and archived flag from storage`() =
        runTest {
            val stored =
                Category(
                    id = 1L,
                    systemKey = "food",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                    sortOrder = 3,
                    isArchived = true,
                )
            categoryRepository.setInitialData(listOf(stored))

            val result = saveCategoryUseCase(stored.copy(color = 2, sortOrder = 0, isArchived = false))

            assertThat(result.isSuccess).isTrue()
            val updated = categoryRepository.getCategory(1L)!!
            assertThat(updated.color).isEqualTo(2)
            assertThat(updated.sortOrder).isEqualTo(3)
            assertThat(updated.isArchived).isTrue()
        }

    @Test
    fun `archived sibling does not block the name`() =
        runTest {
            val archived =
                Category(id = 1L, name = "Mascotas", type = CategoryType.EXPENSE, color = 1, icon = "home", isArchived = true)
            categoryRepository.setInitialData(listOf(archived))

            val result = saveCategoryUseCase(Category(name = "mascotas", type = CategoryType.EXPENSE, color = 1, icon = "home"))

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `fails when another root of the same type already uses the color`() =
        runTest {
            categoryRepository.setInitialData(listOf(category(id = 1L, name = "Comida", color = red)))

            val result = saveCategoryUseCase(category(name = "Ocio", color = red))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.ColorInUse)
        }

    @Test
    fun `same color is allowed in the other type`() =
        runTest {
            categoryRepository.setInitialData(listOf(category(id = 1L, name = "Comida", color = red)))

            val result = saveCategoryUseCase(category(name = "Regalos", color = red, type = CategoryType.INCOME))

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `subcategory may use the color of its parent`() =
        runTest {
            categoryRepository.setInitialData(listOf(category(id = 1L, name = "Comida", color = red)))

            val result = saveCategoryUseCase(category(name = "Restaurantes", color = red, parentId = 1L))

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `subcategory may not use the color of another root`() =
        runTest {
            categoryRepository.setInitialData(
                listOf(
                    category(id = 1L, name = "Comida", color = red),
                    category(id = 2L, name = "Transporte", color = blue),
                ),
            )

            val result = saveCategoryUseCase(category(name = "Gasolina", color = red, parentId = 2L))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.ColorInUse)
        }

    @Test
    fun `archived category does not block its color`() =
        runTest {
            categoryRepository.setInitialData(
                listOf(category(id = 1L, name = "Comida", color = red, isArchived = true)),
            )

            val result = saveCategoryUseCase(category(name = "Ocio", color = red))

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `unchanged color of an existing category saves even when another family repeats it`() =
        runTest {
            categoryRepository.setInitialData(
                listOf(
                    category(id = 1L, name = "Comida", color = red),
                    category(id = 2L, name = "Ocio", color = red),
                ),
            )

            val result = saveCategoryUseCase(categoryRepository.getCategory(2L)!!.copy(name = "Entretenimiento"))

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `moving a subcategory to another root checks its color again`() =
        runTest {
            categoryRepository.setInitialData(
                listOf(
                    category(id = 1L, name = "Comida", color = red),
                    category(id = 2L, name = "Transporte", color = blue),
                    category(id = 3L, name = "Taxi", color = blue, parentId = 2L),
                ),
            )

            val result = saveCategoryUseCase(categoryRepository.getCategory(3L)!!.copy(parentId = 1L))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.ColorInUse)
        }

    @Test
    fun `changing a root color moves only its subcategories that had the old color`() =
        runTest {
            categoryRepository.setInitialData(
                listOf(
                    category(id = 1L, name = "Comida", color = red),
                    category(id = 2L, name = "Restaurantes", color = red, parentId = 1L),
                    category(id = 3L, name = "Super", color = blue, parentId = 1L),
                    category(id = 4L, name = "Ocio", color = red),
                    category(id = 5L, name = "Cine", color = red, parentId = 4L),
                ),
            )

            val result = saveCategoryUseCase(categoryRepository.getCategory(1L)!!.copy(color = green))

            assertThat(result.isSuccess).isTrue()
            assertThat(categoryRepository.getCategory(1L)?.color).isEqualTo(green)
            assertThat(categoryRepository.getCategory(2L)?.color).isEqualTo(green)
            assertThat(categoryRepository.getCategory(3L)?.color).isEqualTo(blue)
            assertThat(categoryRepository.getCategory(4L)?.color).isEqualTo(red)
            assertThat(categoryRepository.getCategory(5L)?.color).isEqualTo(red)
        }

    @Test
    fun `root color change to a color used by another root is rejected and nothing moves`() =
        runTest {
            categoryRepository.setInitialData(
                listOf(
                    category(id = 1L, name = "Comida", color = red),
                    category(id = 2L, name = "Restaurantes", color = red, parentId = 1L),
                    category(id = 3L, name = "Transporte", color = blue),
                ),
            )

            val result = saveCategoryUseCase(categoryRepository.getCategory(1L)!!.copy(color = blue))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.ColorInUse)
            assertThat(categoryRepository.getCategory(1L)?.color).isEqualTo(red)
            assertThat(categoryRepository.getCategory(2L)?.color).isEqualTo(red)
        }

    private fun category(
        id: Long = 0L,
        name: String,
        color: Int,
        type: CategoryType = CategoryType.EXPENSE,
        parentId: Long? = null,
        isArchived: Boolean = false,
    ): Category =
        Category(
            id = id,
            name = name,
            type = type,
            parentId = parentId,
            color = color,
            icon = "restaurant",
            isArchived = isArchived,
        )
}
