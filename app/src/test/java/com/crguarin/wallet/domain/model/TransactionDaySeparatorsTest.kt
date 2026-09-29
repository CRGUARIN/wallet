package com.crguarin.wallet.domain.model

import com.crguarin.wallet.core.money.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TransactionDaySeparatorsTest {
    private val dayOne = LocalDate.of(2026, 9, 28)
    private val dayTwo = LocalDate.of(2026, 9, 27)

    private fun item(
        id: Long,
        date: LocalDate,
    ) = TransactionListEntry.Item(
        TransactionDetails(
            transaction =
                Transaction(
                    id = id,
                    type = TransactionType.EXPENSE,
                    amount = Money(1_000L),
                    accountId = 1L,
                    categoryId = 10L,
                    date = date,
                    createdAt = Instant.EPOCH,
                    updatedAt = Instant.EPOCH,
                ),
            accountName = "Efectivo",
            accountColor = 0,
            accountIcon = "wallet",
        ),
    )

    @Test
    fun `inserts a header before the first item`() {
        val header = TransactionDaySeparators.headerFor(before = null, after = item(1L, dayOne), netByDate = emptyMap())

        assertThat(header).isEqualTo(TransactionListEntry.DayHeader(dayOne, Money.ZERO))
    }

    @Test
    fun `inserts a header when the day changes`() {
        val header =
            TransactionDaySeparators.headerFor(
                before = item(1L, dayOne),
                after = item(2L, dayTwo),
                netByDate = mapOf(dayTwo to Money(5_000L)),
            )

        assertThat(header).isEqualTo(TransactionListEntry.DayHeader(dayTwo, Money(5_000L)))
    }

    @Test
    fun `does not insert a header for items on the same day`() {
        val header =
            TransactionDaySeparators.headerFor(
                before = item(1L, dayOne),
                after = item(2L, dayOne),
                netByDate = emptyMap(),
            )

        assertThat(header).isNull()
    }

    @Test
    fun `does not insert a header for the terminal position`() {
        val header = TransactionDaySeparators.headerFor(before = item(1L, dayOne), after = null, netByDate = emptyMap())

        assertThat(header).isNull()
    }

    @Test
    fun `uses the net from the provided map and falls back to zero`() {
        val withNet =
            TransactionDaySeparators.headerFor(
                before = null,
                after = item(1L, dayOne),
                netByDate = mapOf(dayOne to Money(-3_000L)),
            )
        val withoutNet =
            TransactionDaySeparators.headerFor(
                before = null,
                after = item(1L, dayOne),
                netByDate = emptyMap(),
            )

        assertThat(withNet?.net).isEqualTo(Money(-3_000L))
        assertThat(withoutNet?.net).isEqualTo(Money.ZERO)
    }
}
