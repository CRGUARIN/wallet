package com.crguarin.wallet.data.db

import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {
    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun accountTypeConverters_roundTrip() {
        assertThat(converters.fromAccountType(AccountType.CASH)).isEqualTo("CASH")
        assertThat(converters.fromAccountType(AccountType.SAVINGS)).isEqualTo("SAVINGS")
        assertThat(converters.fromAccountType(null)).isNull()

        assertThat(converters.toAccountType("CASH")).isEqualTo(AccountType.CASH)
        assertThat(converters.toAccountType("SAVINGS")).isEqualTo(AccountType.SAVINGS)
        assertThat(converters.toAccountType(null)).isNull()
    }

    @Test
    fun categoryTypeConverters_roundTrip() {
        assertThat(converters.fromCategoryType(CategoryType.INCOME)).isEqualTo("INCOME")
        assertThat(converters.fromCategoryType(CategoryType.EXPENSE)).isEqualTo("EXPENSE")
        assertThat(converters.fromCategoryType(null)).isNull()

        assertThat(converters.toCategoryType("INCOME")).isEqualTo(CategoryType.INCOME)
        assertThat(converters.toCategoryType("EXPENSE")).isEqualTo(CategoryType.EXPENSE)
        assertThat(converters.toCategoryType(null)).isNull()
    }

    @Test
    fun transactionTypeConverters_roundTrip() {
        assertThat(converters.fromTransactionType(TransactionType.INCOME)).isEqualTo("INCOME")
        assertThat(converters.fromTransactionType(TransactionType.EXPENSE)).isEqualTo("EXPENSE")
        assertThat(converters.fromTransactionType(TransactionType.TRANSFER)).isEqualTo("TRANSFER")
        assertThat(converters.fromTransactionType(null)).isNull()

        assertThat(converters.toTransactionType("INCOME")).isEqualTo(TransactionType.INCOME)
        assertThat(converters.toTransactionType("EXPENSE")).isEqualTo(TransactionType.EXPENSE)
        assertThat(converters.toTransactionType("TRANSFER")).isEqualTo(TransactionType.TRANSFER)
        assertThat(converters.toTransactionType(null)).isNull()
    }

    @Test
    fun localDateConverters_roundTrip() {
        val date = LocalDate.of(2026, 9, 28)
        val epochDay = converters.fromLocalDate(date)
        assertThat(epochDay).isEqualTo(date.toEpochDay())
        assertThat(converters.toLocalDate(epochDay)).isEqualTo(date)

        val epochZero = LocalDate.ofEpochDay(0)
        assertThat(converters.toLocalDate(converters.fromLocalDate(epochZero))).isEqualTo(epochZero)

        assertThat(converters.fromLocalDate(null)).isNull()
        assertThat(converters.toLocalDate(null)).isNull()
    }

    @Test
    fun instantConverters_roundTrip() {
        val instant = Instant.ofEpochMilli(1790000000000L)
        val epochMillis = converters.fromInstant(instant)
        assertThat(epochMillis).isEqualTo(1790000000000L)
        assertThat(converters.toInstant(epochMillis)).isEqualTo(instant)

        val epochZero = Instant.ofEpochMilli(0)
        assertThat(converters.toInstant(converters.fromInstant(epochZero))).isEqualTo(epochZero)

        assertThat(converters.fromInstant(null)).isNull()
        assertThat(converters.toInstant(null)).isNull()
    }
}
