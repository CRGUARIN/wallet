package com.crguarin.wallet.data.backup

import com.crguarin.wallet.core.money.APP_CURRENCY
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.TransactionType
import java.time.DateTimeException
import java.time.LocalDate

/**
 * Pure validation of a decoded [BackupDto]. Runs before the database is touched, so it must
 * reject anything that would violate a business rule or a foreign key on insert.
 */
object BackupValidator {
    /** Returns null when [dto] is valid, or the [DomainError] to report otherwise. */
    fun validate(dto: BackupDto): DomainError? {
        if (dto.schemaVersion != BackupDto.SCHEMA_VERSION) return DomainError.BackupVersionUnsupported
        return firstReason(dto)?.let { DomainError.BackupInvalid(it) }
    }

    private fun firstReason(dto: BackupDto): String? {
        if (dto.currency != APP_CURRENCY) return "unsupported currency '${dto.currency}'"
        return validateAccounts(dto.accounts)
            ?: validateCategories(dto.categories)
            ?: validateTransactions(dto)
    }

    private fun validateAccounts(accounts: List<AccountDto>): String? {
        idProblem("account", accounts.map { it.id })?.let { return it }
        for (account in accounts) {
            val name = account.name.trim()
            when {
                account.currencyCode != APP_CURRENCY -> return "account ${account.id}: unsupported currency"
                name.isEmpty() -> return "account ${account.id}: empty name"
                name.length > DomainLimits.NAME_MAX_LENGTH -> return "account ${account.id}: name too long"
                account.initialBalance < 0 -> return "account ${account.id}: negative initial balance"
                account.initialBalance > Money.MAX_MINOR_UNITS -> return "account ${account.id}: initial balance too large"
            }
        }
        return null
    }

    private fun validateCategories(categories: List<CategoryDto>): String? {
        idProblem("category", categories.map { it.id })?.let { return it }
        val byId = categories.associateBy { it.id }
        for (category in categories) {
            if (category.name == null && category.systemKey == null) return "category ${category.id}: no name or systemKey"
            if (category.name != null && category.name.isBlank()) return "category ${category.id}: blank name"
            val parentId = category.parentId ?: continue
            val parent = byId[parentId] ?: return "category ${category.id}: unknown parent $parentId"
            when {
                parentId == category.id -> return "category ${category.id}: is its own parent"
                parent.parentId != null -> return "category ${category.id}: more than 2 levels"
                parent.type != category.type -> return "category ${category.id}: type differs from parent"
            }
        }
        return null
    }

    private fun validateTransactions(dto: BackupDto): String? {
        idProblem("transaction", dto.transactions.map { it.id })?.let { return it }
        val accountIds = dto.accounts.map { it.id }.toSet()
        val categoriesById = dto.categories.associateBy { it.id }
        return dto.transactions.firstNotNullOfOrNull { transactionProblem(it, accountIds, categoriesById) }
    }

    private fun transactionProblem(
        tx: TransactionDto,
        accountIds: Set<Long>,
        categoriesById: Map<Long, CategoryDto>,
    ): String? {
        val label = "transaction ${tx.id}"
        return when {
            tx.amount <= 0 -> "$label: amount must be positive"
            tx.amount > Money.MAX_MINOR_UNITS -> "$label: amount too large"
            !isValidDate(tx.date) -> "$label: invalid date"
            (tx.note?.length ?: 0) > DomainLimits.NOTE_MAX_LENGTH -> "$label: note too long"
            tx.accountId !in accountIds -> "$label: unknown account ${tx.accountId}"
            tx.type == TransactionType.TRANSFER -> transferProblem(tx, accountIds)
            else -> flowProblem(tx, categoriesById)
        }
    }

    private fun transferProblem(
        tx: TransactionDto,
        accountIds: Set<Long>,
    ): String? {
        val label = "transaction ${tx.id}"
        val to = tx.toAccountId
        return when {
            tx.categoryId != null -> "$label: transfer with category"
            to == null -> "$label: transfer without destination"
            to == tx.accountId -> "$label: transfer to the same account"
            to !in accountIds -> "$label: unknown destination account $to"
            else -> null
        }
    }

    private fun flowProblem(
        tx: TransactionDto,
        categoriesById: Map<Long, CategoryDto>,
    ): String? {
        val label = "transaction ${tx.id}"
        val categoryId = tx.categoryId
        val category = categoryId?.let { categoriesById[it] }
        return when {
            tx.toAccountId != null -> "$label: ${tx.type} with destination account"
            categoryId == null -> "$label: ${tx.type} without category"
            category == null -> "$label: unknown category $categoryId"
            category.type.name != tx.type.name -> "$label: category type differs from transaction type"
            else -> null
        }
    }

    private fun idProblem(
        entity: String,
        ids: List<Long>,
    ): String? =
        when {
            ids.any { it <= 0 } -> "$entity with non-positive id"
            ids.toSet().size != ids.size -> "duplicated $entity id"
            else -> null
        }

    private fun isValidDate(text: String): Boolean =
        try {
            LocalDate.parse(text)
            true
        } catch (_: DateTimeException) {
            false
        }
}
