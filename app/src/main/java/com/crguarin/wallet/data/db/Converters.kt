package com.crguarin.wallet.data.db

import androidx.room.TypeConverter
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? = value?.let { AccountType.valueOf(it) }

    @TypeConverter
    fun fromCategoryType(value: CategoryType?): String? = value?.name

    @TypeConverter
    fun toCategoryType(value: String?): CategoryType? = value?.let { CategoryType.valueOf(it) }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun fromRecurrenceFrequency(value: RecurrenceFrequency?): String? = value?.name

    @TypeConverter
    fun toRecurrenceFrequency(value: String?): RecurrenceFrequency? = value?.let { RecurrenceFrequency.valueOf(it) }

    @TypeConverter
    fun fromRecurringMode(value: RecurringMode?): String? = value?.name

    @TypeConverter
    fun toRecurringMode(value: String?): RecurringMode? = value?.let { RecurringMode.valueOf(it) }

    @TypeConverter
    fun fromDebtDirection(value: DebtDirection?): String? = value?.name

    @TypeConverter
    fun toDebtDirection(value: String?): DebtDirection? = value?.let { DebtDirection.valueOf(it) }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}
