package com.crguarin.wallet.domain.usecase.report

import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeReportRepository
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.CategorySlice
import com.crguarin.wallet.domain.model.report.CategoryTotal
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.crguarin.wallet.domain.model.report.SliceKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ObserveCategoryBreakdownUseCaseTest {
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var useCase: ObserveCategoryBreakdownUseCase

    private val period = ReportPeriod.Month(YearMonth.of(2026, 9))

    @Before
    fun setUp() {
        reportRepository = FakeReportRepository()
        useCase = ObserveCategoryBreakdownUseCase(reportRepository)
    }

    @Test
    fun `root breakdown sums subcategories into their parent and marks hasChildren`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotals =
                    listOf(
                        total(1L, "Alimentación", amount = 10L, color = RED),
                        total(2L, "Mercado", parentId = 1L, parentName = "Alimentación", amount = 60L, parentColor = RED),
                        total(3L, "Restaurantes", parentId = 1L, parentName = "Alimentación", amount = 30L, parentColor = RED),
                        total(4L, "Transporte", amount = 40L, color = BLUE),
                        total(null, null, amount = 20L),
                    ),
            )

            useCase(period, TransactionType.EXPENSE, accountId = null).test {
                val breakdown = awaitItem()

                assertThat(breakdown.parentId).isNull()
                assertThat(breakdown.total).isEqualTo(Money(160L))
                assertThat(breakdown.slices.map { it.key })
                    .containsExactly(SliceKey.Category(1L), SliceKey.Category(4L), SliceKey.Uncategorized)
                    .inOrder()
                assertThat(breakdown.slices.map { it.amount })
                    .containsExactly(Money(100L), Money(40L), Money(20L))
                    .inOrder()
                assertThat(breakdown.slices.map { it.perMille }).containsExactly(625, 250, 125).inOrder()

                val food = breakdown.slices[0]
                assertThat(food.name).isEqualTo("Alimentación")
                assertThat(food.color).isEqualTo(RED)
                assertThat(food.hasChildren).isTrue()

                val transport = breakdown.slices[1]
                assertThat(transport.name).isEqualTo("Transporte")
                assertThat(transport.hasChildren).isFalse()

                val uncategorized = breakdown.slices[2]
                assertThat(uncategorized.name).isNull()
                assertThat(uncategorized.hasChildren).isFalse()

                assertThat(reportRepository.categoryRangeRequests)
                    .containsExactly(LocalDate.of(2026, 9, 1) to LocalDate.of(2026, 9, 30))
                    .inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `drill down shows the subcategories plus the parent direct slice`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotals =
                    listOf(
                        total(1L, "Alimentación", amount = 10L),
                        total(2L, "Mercado", parentId = 1L, parentName = "Alimentación", amount = 60L),
                        total(3L, "Restaurantes", parentId = 1L, parentName = "Alimentación", amount = 30L),
                        total(4L, "Transporte", amount = 40L),
                    ),
            )

            useCase(period, TransactionType.EXPENSE, accountId = null, parentId = 1L).test {
                val breakdown = awaitItem()

                assertThat(breakdown.parentId).isEqualTo(1L)
                assertThat(breakdown.total).isEqualTo(Money(100L))
                assertThat(breakdown.slices.map { it.key })
                    .containsExactly(
                        SliceKey.Category(2L),
                        SliceKey.Category(3L),
                        SliceKey.ParentDirect(1L),
                    ).inOrder()
                assertThat(breakdown.slices.map { it.amount })
                    .containsExactly(Money(60L), Money(30L), Money(10L))
                    .inOrder()
                assertThat(breakdown.slices.map { it.perMille }).containsExactly(600, 300, 100).inOrder()
                assertThat(breakdown.slices.any { it.hasChildren }).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `drill down omits the parent direct slice when there are no direct movements`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotals =
                    listOf(
                        total(2L, "Mercado", parentId = 1L, parentName = "Alimentación", amount = 60L),
                        total(3L, "Restaurantes", parentId = 1L, parentName = "Alimentación", amount = 30L),
                    ),
            )

            useCase(period, TransactionType.EXPENSE, accountId = null, parentId = 1L).test {
                val breakdown = awaitItem()

                assertThat(breakdown.slices.map { it.key })
                    .containsExactly(SliceKey.Category(2L), SliceKey.Category(3L))
                    .inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `groups the smallest slices into Others when there are more than maxSlices`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotals =
                    listOf(
                        total(1L, "Cat 1", amount = 100L),
                        total(2L, "Cat 2", amount = 80L),
                        total(3L, "Cat 3", amount = 60L),
                        total(4L, "Cat 4", amount = 50L),
                        total(5L, "Cat 5", amount = 40L),
                        total(6L, "Cat 6", amount = 30L),
                        total(7L, "Cat 7", amount = 20L),
                    ),
            )

            useCase(period, TransactionType.EXPENSE, accountId = null).test {
                val breakdown = awaitItem()

                assertThat(breakdown.total).isEqualTo(Money(380L))
                assertThat(breakdown.slices).hasSize(6)
                assertThat(breakdown.slices.last().key).isEqualTo(SliceKey.Others)
                assertThat(breakdown.slices.last().name).isNull()
                assertThat(breakdown.slices.last().amount).isEqualTo(Money(50L))
                assertThat(breakdown.slices.last().hasChildren).isFalse()
                assertThat(breakdown.slices.take(5).map { it.amount })
                    .containsExactly(Money(100L), Money(80L), Money(60L), Money(50L), Money(40L))
                    .inOrder()
                assertThat(breakdown.slices.map { it.amount.minorUnits }.sum()).isEqualTo(380L)
                assertThat(breakdown.slices.map { it.perMille }.sum()).isEqualTo(1000)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `does not group when there are exactly maxSlices slices`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotals =
                    listOf(
                        total(1L, "Cat 1", amount = 60L),
                        total(2L, "Cat 2", amount = 50L),
                        total(3L, "Cat 3", amount = 40L),
                        total(4L, "Cat 4", amount = 30L),
                        total(5L, "Cat 5", amount = 20L),
                        total(6L, "Cat 6", amount = 10L),
                    ),
            )

            useCase(period, TransactionType.EXPENSE, accountId = null).test {
                val breakdown = awaitItem()

                assertThat(breakdown.slices).hasSize(6)
                assertThat(breakdown.slices.map { it.key }).doesNotContain(SliceKey.Others)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `uses a custom maxSlices to keep the largest slices plus Others`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotals =
                    listOf(
                        total(1L, "Cat 1", amount = 100L),
                        total(2L, "Cat 2", amount = 80L),
                        total(3L, "Cat 3", amount = 60L),
                        total(4L, "Cat 4", amount = 40L),
                    ),
            )

            useCase(period, TransactionType.EXPENSE, accountId = null, maxSlices = 3).test {
                val breakdown = awaitItem()

                assertThat(breakdown.slices.map { it.key })
                    .containsExactly(SliceKey.Category(1L), SliceKey.Category(2L), SliceKey.Others)
                    .inOrder()
                assertThat(breakdown.slices.last().amount).isEqualTo(Money(100L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sorts ties by name and then by id`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotals =
                    listOf(
                        total(10L, "Zeta", amount = 50L),
                        total(11L, "Alfa", amount = 50L),
                        total(12L, "Alfa", amount = 50L),
                    ),
            )

            useCase(period, TransactionType.EXPENSE, accountId = null).test {
                val breakdown = awaitItem()

                assertThat(breakdown.slices.map { it.key })
                    .containsExactly(SliceKey.Category(11L), SliceKey.Category(12L), SliceKey.Category(10L))
                    .inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `builds a slice for uncategorized movements`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotals = listOf(total(null, null, amount = 75L)),
            )

            useCase(period, TransactionType.EXPENSE, accountId = null).test {
                val breakdown = awaitItem()

                assertThat(breakdown.total).isEqualTo(Money(75L))
                assertThat(breakdown.slices).hasSize(1)
                assertThat(breakdown.slices.first().key).isEqualTo(SliceKey.Uncategorized)
                assertThat(breakdown.slices.first().perMille).isEqualTo(1000)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `returns an empty breakdown when the period has no movements`() =
        runTest {
            useCase(period, TransactionType.EXPENSE, accountId = null).test {
                val breakdown = awaitItem()

                assertThat(breakdown.total).isEqualTo(Money.ZERO)
                assertThat(breakdown.slices).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `root slice of a system category keeps the parent fields when only a child has movements`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotals =
                    listOf(
                        total(
                            categoryId = 2L,
                            name = "Mercado",
                            systemKey = "groceries",
                            amount = 60L,
                            color = GREEN,
                            icon = "cart",
                            parentId = 1L,
                            parentName = null,
                            parentSystemKey = "food",
                            parentColor = RED,
                            parentIcon = "food",
                        ),
                    ),
            )

            useCase(period, TransactionType.EXPENSE, accountId = null).test {
                val breakdown = awaitItem()

                assertThat(breakdown.slices).hasSize(1)
                val slice = breakdown.slices.single()
                assertThat(slice.key).isEqualTo(SliceKey.Category(1L))
                assertThat(slice.name).isNull()
                assertThat(slice.systemKey).isEqualTo("food")
                assertThat(slice.color).isEqualTo(RED)
                assertThat(slice.icon).isEqualTo("food")
                assertThat(slice.amount).isEqualTo(Money(60L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `root slice of a system category is the same regardless of row order`() =
        runTest {
            val direct =
                total(
                    categoryId = 1L,
                    name = null,
                    systemKey = "food",
                    amount = 10L,
                    color = RED,
                    icon = "food",
                )
            val child =
                total(
                    categoryId = 2L,
                    name = "Mercado",
                    systemKey = "groceries",
                    amount = 60L,
                    color = GREEN,
                    icon = "cart",
                    parentId = 1L,
                    parentName = null,
                    parentSystemKey = "food",
                    parentColor = RED,
                    parentIcon = "food",
                )

            val firstDirect = breakdownOf(listOf(direct, child))
            val firstChild = breakdownOf(listOf(child, direct))

            assertThat(firstChild.map { it.name }).isEqualTo(firstDirect.map { it.name })
            assertThat(firstChild.map { it.systemKey }).isEqualTo(firstDirect.map { it.systemKey })
            assertThat(firstChild.map { it.color }).isEqualTo(firstDirect.map { it.color })
            assertThat(firstChild.map { it.icon }).isEqualTo(firstDirect.map { it.icon })
            assertThat(firstDirect.single().systemKey).isEqualTo("food")
            assertThat(firstDirect.single().name).isNull()
        }

    @Test
    fun `groups exactly two smallest slices into Others with seven slices and maxSlices six`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotals =
                    listOf(
                        total(1L, "Cat 1", amount = 100L),
                        total(2L, "Cat 2", amount = 80L),
                        total(3L, "Cat 3", amount = 60L),
                        total(4L, "Cat 4", amount = 50L),
                        total(5L, "Cat 5", amount = 40L),
                        total(6L, "Cat 6", amount = 30L),
                        total(7L, "Cat 7", amount = 20L),
                    ),
            )

            useCase(period, TransactionType.EXPENSE, accountId = null, maxSlices = 6).test {
                val breakdown = awaitItem()

                assertThat(breakdown.slices).hasSize(6)
                assertThat(breakdown.slices.take(5).map { it.amount })
                    .containsExactly(Money(100L), Money(80L), Money(60L), Money(50L), Money(40L))
                    .inOrder()
                val others = breakdown.slices.last()
                assertThat(others.key).isEqualTo(SliceKey.Others)
                assertThat(others.amount).isEqualTo(Money(50L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `forwards the accountId and type to the repository`() =
        runTest {
            reportRepository.setInitialData(
                categoryTotalsByType =
                    mapOf(
                        TransactionType.EXPENSE to listOf(total(1L, "Cat 1", amount = 10L)),
                        TransactionType.INCOME to listOf(total(2L, "Cat 2", amount = 20L)),
                    ),
            )

            useCase(period, TransactionType.EXPENSE, accountId = 7L).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.categoryTypeRequests)
                .containsExactly(TransactionType.EXPENSE)
            assertThat(reportRepository.categoryAccountRequests).containsExactly(7L)
        }

    @Test
    fun `rejects transfers like the real repository`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(period, TransactionType.TRANSFER, accountId = null)
        }
    }

    private suspend fun breakdownOf(rows: List<CategoryTotal>): List<CategorySlice> {
        reportRepository.setInitialData(categoryTotals = rows)
        return useCase(period, TransactionType.EXPENSE, accountId = null).first().slices
    }

    private fun total(
        categoryId: Long?,
        name: String?,
        amount: Long,
        parentId: Long? = null,
        parentName: String? = null,
        color: Int? = null,
        parentColor: Int? = null,
        systemKey: String? = null,
        icon: String? = null,
        parentSystemKey: String? = null,
        parentIcon: String? = null,
        transactionCount: Int = 1,
    ): CategoryTotal =
        CategoryTotal(
            categoryId = categoryId,
            name = name,
            systemKey = systemKey,
            color = color,
            icon = icon,
            parentId = parentId,
            parentName = parentName,
            parentSystemKey = parentSystemKey,
            parentColor = parentColor,
            parentIcon = parentIcon,
            total = Money(amount),
            transactionCount = transactionCount,
        )

    private companion object {
        private const val RED = 0xFFFF0000.toInt()
        private const val BLUE = 0xFF0000FF.toInt()
        private const val GREEN = 0xFF00FF00.toInt()
    }
}
