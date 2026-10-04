package com.crguarin.wallet.data.backup

import com.crguarin.wallet.core.money.APP_CURRENCY
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.TransactionType
import java.time.DateTimeException
import java.time.LocalDate

/**
 * Pure validation of a decoded [BackupDto]. Runs before the database is touched, so it must
 * reject anything that would violate a business rule or a foreign key on insert.
 *
 * Accepts schema versions 1, 2 and 3: a version 1 backup carries no card fields and is
 * imported with the defaults (no credit cards, no statements); versions 1 and 2 carry no
 * recurring payments and are imported without them.
 */
object BackupValidator {
    /** Returns null when [dto] is valid, or the [DomainError] to report otherwise. */
    fun validate(dto: BackupDto): DomainError? {
        if (dto.schemaVersion != 1 && dto.schemaVersion != 2 && dto.schemaVersion != BackupDto.SCHEMA_VERSION) {
            return DomainError.BackupVersionUnsupported
        }
        return firstReason(dto)?.let { DomainError.BackupInvalid(it) }
    }

    private fun firstReason(dto: BackupDto): String? {
        if (dto.currency != APP_CURRENCY) return "unsupported currency '${dto.currency}'"
        val accountTypes = dto.accounts.associate { it.id to it.type }
        val recurringIds = dto.recurringPayments.map { it.id }.toSet()
        return validateAccounts(dto.accounts)
            ?: validateCategories(dto.categories)
            ?: validateRecurringPayments(dto)
            ?: validateTransactions(dto, accountTypes, recurringIds)
            ?: validateCardStatements(dto, accountTypes)
    }

    private fun validateAccounts(accounts: List<AccountDto>): String? {
        idProblem("account", accounts.map { it.id })?.let { return it }
        for (account in accounts) {
            val name = account.name.trim()
            when {
                account.currencyCode != APP_CURRENCY -> return "account ${account.id}: unsupported currency"
                name.isEmpty() -> return "account ${account.id}: empty name"
                name.length > DomainLimits.NAME_MAX_LENGTH -> return "account ${account.id}: name too long"
                account.initialBalance > Money.MAX_MINOR_UNITS -> return "account ${account.id}: initial balance too large"
                account.initialBalance < -Money.MAX_MINOR_UNITS -> return "account ${account.id}: initial balance too large"
                account.initialBalance < 0 && account.type != AccountType.CREDIT_CARD ->
                    return "account ${account.id}: negative initial balance"
            }
            accountCardProblem(account)?.let { return it }
        }
        return null
    }

    private fun accountCardProblem(account: AccountDto): String? {
        val label = "account ${account.id}"
        return if (account.type == AccountType.CREDIT_CARD) {
            when {
                account.creditLimit == null -> "$label: card without credit limit"
                account.creditLimit <= 0 -> "$label: credit limit must be positive"
                account.creditLimit > Money.MAX_MINOR_UNITS -> "$label: credit limit too large"
                account.statementDay == null || account.statementDay !in 1..31 -> "$label: invalid statement day"
                account.paymentDueDay == null || account.paymentDueDay !in 1..31 -> "$label: invalid payment due day"
                else -> null
            }
        } else {
            when {
                account.creditLimit != null -> "$label: credit limit on a non-card account"
                account.statementDay != null -> "$label: statement day on a non-card account"
                account.paymentDueDay != null -> "$label: payment due day on a non-card account"
                else -> null
            }
        }
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

    private fun validateRecurringPayments(dto: BackupDto): String? {
        val payments = dto.recurringPayments
        idProblem("recurring payment", payments.map { it.id })?.let { return it }
        val accountIds = dto.accounts.map { it.id }.toSet()
        val categoriesById = dto.categories.associateBy { it.id }
        return payments.firstNotNullOfOrNull { recurringProblem(it, accountIds, categoriesById) }
    }

    private fun recurringProblem(
        payment: RecurringPaymentDto,
        accountIds: Set<Long>,
        categoriesById: Map<Long, CategoryDto>,
    ): String? {
        val label = "recurring payment ${payment.id}"
        val category = categoriesById[payment.categoryId]
        return when {
            payment.name.trim().isEmpty() -> "$label: empty name"
            payment.name.length > DomainLimits.NAME_MAX_LENGTH -> "$label: name too long"
            payment.type == TransactionType.TRANSFER -> "$label: invalid type"
            payment.amount <= 0 -> "$label: amount must be positive"
            payment.amount > Money.MAX_MINOR_UNITS -> "$label: amount too large"
            (payment.note?.length ?: 0) > DomainLimits.NOTE_MAX_LENGTH -> "$label: note too long"
            payment.accountId !in accountIds -> "$label: unknown account ${payment.accountId}"
            category == null -> "$label: unknown category ${payment.categoryId}"
            category.type.name != payment.type.name -> "$label: category type differs from payment type"
            payment.interval !in 1..12 -> "$label: invalid interval"
            payment.reminderDaysBefore != null && payment.reminderDaysBefore !in 1..7 ->
                "$label: invalid reminder"
            !isValidDate(payment.startDate) -> "$label: invalid start date"
            !isValidDate(payment.nextDueDate) -> "$label: invalid next due date"
            payment.endDate != null && !isValidDate(payment.endDate) -> "$label: invalid end date"
            payment.endDate != null && LocalDate.parse(payment.endDate) < LocalDate.parse(payment.startDate) ->
                "$label: end date before start date"
            else -> null
        }
    }

    private fun validateTransactions(
        dto: BackupDto,
        accountTypes: Map<Long, AccountType>,
        recurringIds: Set<Long>,
    ): String? {
        idProblem("transaction", dto.transactions.map { it.id })?.let { return it }
        val accountIds = dto.accounts.map { it.id }.toSet()
        val categoriesById = dto.categories.associateBy { it.id }
        return dto.transactions.firstNotNullOfOrNull {
            transactionProblem(it, accountIds, accountTypes, categoriesById, recurringIds)
        }
    }

    private fun transactionProblem(
        tx: TransactionDto,
        accountIds: Set<Long>,
        accountTypes: Map<Long, AccountType>,
        categoriesById: Map<Long, CategoryDto>,
        recurringIds: Set<Long>,
    ): String? {
        val label = "transaction ${tx.id}"
        return when {
            tx.amount <= 0 -> "$label: amount must be positive"
            tx.amount > Money.MAX_MINOR_UNITS -> "$label: amount too large"
            !isValidDate(tx.date) -> "$label: invalid date"
            (tx.note?.length ?: 0) > DomainLimits.NOTE_MAX_LENGTH -> "$label: note too long"
            tx.accountId !in accountIds -> "$label: unknown account ${tx.accountId}"
            tx.recurringId != null && tx.recurringId !in recurringIds ->
                "$label: unknown recurring payment ${tx.recurringId}"
            !isValidInstallments(tx, accountTypes) -> "$label: invalid installments"
            tx.type == TransactionType.TRANSFER -> transferProblem(tx, accountIds)
            else -> flowProblem(tx, categoriesById)
        }
    }

    private fun isValidInstallments(
        tx: TransactionDto,
        accountTypes: Map<Long, AccountType>,
    ): Boolean {
        val installments = tx.installments ?: return true
        return tx.type == TransactionType.EXPENSE &&
            accountTypes[tx.accountId] == AccountType.CREDIT_CARD &&
            installments in 1..48
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

    private fun validateCardStatements(
        dto: BackupDto,
        accountTypes: Map<Long, AccountType>,
    ): String? {
        val statements = dto.cardStatements
        idProblem("card statement", statements.map { it.id })?.let { return it }
        val seenClosings = mutableSetOf<Pair<Long, String>>()
        val transactionsById = dto.transactions.associateBy { it.id }
        for (statement in statements) {
            val label = "card statement ${statement.id}"
            val accountType = accountTypes[statement.accountId]
            when {
                accountType == null -> return "$label: unknown account ${statement.accountId}"
                accountType != AccountType.CREDIT_CARD -> return "$label: account ${statement.accountId} is not a credit card"
                !isValidDate(statement.closingDate) -> return "$label: invalid closing date"
                !isValidDate(statement.dueDate) -> return "$label: invalid due date"
                !seenClosings.add(statement.accountId to statement.closingDate) ->
                    return "$label: duplicated closing date for account ${statement.accountId}"
                statement.statementBalance < 0 -> return "$label: negative statement balance"
                statement.statementBalance > Money.MAX_MINOR_UNITS -> return "$label: statement balance too large"
                !isValidOptionalAmount(statement.minimumPayment) -> return "$label: invalid minimum payment"
                !isValidOptionalAmount(statement.statedInterestAndFees) -> return "$label: invalid stated interest and fees"
                !isValidAdjustment(statement, transactionsById) -> return "$label: unknown adjustment ${statement.adjustmentTransactionId}"
            }
        }
        return null
    }

    private fun isValidOptionalAmount(amount: Long?): Boolean = amount == null || amount in 0..Money.MAX_MINOR_UNITS

    private fun isValidAdjustment(
        statement: CardStatementDto,
        transactionsById: Map<Long, TransactionDto>,
    ): Boolean {
        val adjustmentId = statement.adjustmentTransactionId ?: return true
        val transaction = transactionsById[adjustmentId] ?: return false
        return transaction.accountId == statement.accountId
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
