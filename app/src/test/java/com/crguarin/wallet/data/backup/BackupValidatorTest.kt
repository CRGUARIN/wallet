package com.crguarin.wallet.data.backup

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class BackupValidatorTest {
    private val valid = validBackupDto()

    private fun assertInvalid(dto: BackupDto) {
        assertThat(BackupValidator.validate(dto)).isInstanceOf(DomainError.BackupInvalid::class.java)
    }

    private fun BackupDto.withAccount(
        index: Int = 0,
        change: (AccountDto) -> AccountDto,
    ) = copy(accounts = accounts.mapIndexed { i, a -> if (i == index) change(a) else a })

    private fun BackupDto.withCategory(
        id: Long,
        change: (CategoryDto) -> CategoryDto,
    ) = copy(categories = categories.map { if (it.id == id) change(it) else it })

    private fun BackupDto.withTransaction(
        id: Long,
        change: (TransactionDto) -> TransactionDto,
    ) = copy(transactions = transactions.map { if (it.id == id) change(it) else it })

    @Test
    fun `valid backup passes`() {
        assertThat(BackupValidator.validate(valid)).isNull()
    }

    @Test
    fun `empty backup passes`() {
        assertThat(
            BackupValidator.validate(valid.copy(accounts = emptyList(), categories = emptyList(), transactions = emptyList())),
        ).isNull()
    }

    @Test
    fun `unsupported schema version is reported as such`() {
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 6))).isEqualTo(DomainError.BackupVersionUnsupported)
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 0))).isEqualTo(DomainError.BackupVersionUnsupported)
    }

    @Test
    fun `schema versions 1 to 5 are accepted`() {
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 1))).isNull()
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 2))).isNull()
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 3))).isNull()
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 4))).isNull()
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 5))).isNull()
        assertThat(BackupValidator.validate(validCardBackupDto())).isNull()
        assertThat(BackupValidator.validate(validRecurringBackupDto())).isNull()
        assertThat(BackupValidator.validate(validDebtBackupDto())).isNull()
        assertThat(BackupValidator.validate(validTransitBackupDto())).isNull()
    }

    @Test
    fun `currency other than COP is rejected`() {
        assertInvalid(valid.copy(currency = "USD"))
    }

    @Test
    fun `account currency other than COP is rejected`() {
        assertInvalid(valid.withAccount { it.copy(currencyCode = "USD") })
    }

    @Test
    fun `non positive ids are rejected in every list`() {
        assertInvalid(valid.withAccount { it.copy(id = 0) })
        assertInvalid(valid.withCategory(1) { it.copy(id = -1) })
        assertInvalid(valid.withTransaction(1) { it.copy(id = 0) })
    }

    @Test
    fun `duplicated ids are rejected in every list`() {
        assertInvalid(valid.withAccount(1) { it.copy(id = 1) })
        assertInvalid(valid.withCategory(3) { it.copy(id = 1, parentId = null) })
        assertInvalid(valid.withTransaction(2) { it.copy(id = 1) })
    }

    @Test
    fun `same id in different lists is fine`() {
        assertThat(valid.accounts.map { it.id }).containsAnyIn(valid.categories.map { it.id })
        assertThat(BackupValidator.validate(valid)).isNull()
    }

    @Test
    fun `transaction pointing to an unknown account is rejected`() {
        assertInvalid(valid.withTransaction(1) { it.copy(accountId = 99) })
    }

    @Test
    fun `transfer pointing to an unknown destination is rejected`() {
        assertInvalid(valid.withTransaction(3) { it.copy(toAccountId = 99) })
    }

    @Test
    fun `transaction pointing to an unknown category is rejected`() {
        assertInvalid(valid.withTransaction(1) { it.copy(categoryId = 99) })
    }

    @Test
    fun `category pointing to an unknown parent is rejected`() {
        assertInvalid(valid.withCategory(3) { it.copy(parentId = 99) })
    }

    @Test
    fun `zero and negative amounts are rejected`() {
        assertInvalid(valid.withTransaction(1) { it.copy(amount = 0) })
        assertInvalid(valid.withTransaction(1) { it.copy(amount = -5) })
    }

    @Test
    fun `amount above the money parser maximum is rejected but the maximum itself is valid`() {
        assertInvalid(valid.withTransaction(1) { it.copy(amount = Money.MAX_MINOR_UNITS + 1) })
        assertThat(BackupValidator.validate(valid.withTransaction(1) { it.copy(amount = Money.MAX_MINOR_UNITS) })).isNull()
    }

    @Test
    fun `negative initial balance is rejected and zero is valid`() {
        assertInvalid(valid.withAccount { it.copy(initialBalance = -1) })
        assertThat(BackupValidator.validate(valid.withAccount { it.copy(initialBalance = 0) })).isNull()
    }

    @Test
    fun `initial balance above the maximum is rejected`() {
        assertInvalid(valid.withAccount { it.copy(initialBalance = Money.MAX_MINOR_UNITS + 1) })
    }

    @Test
    fun `income without category is rejected`() {
        assertInvalid(valid.withTransaction(2) { it.copy(categoryId = null) })
    }

    @Test
    fun `expense without category is rejected`() {
        assertInvalid(valid.withTransaction(1) { it.copy(categoryId = null) })
    }

    @Test
    fun `income or expense with a destination account is rejected`() {
        assertInvalid(valid.withTransaction(1) { it.copy(toAccountId = 2) })
        assertInvalid(valid.withTransaction(2) { it.copy(toAccountId = 1) })
    }

    @Test
    fun `transfer without destination is rejected`() {
        assertInvalid(valid.withTransaction(3) { it.copy(toAccountId = null) })
    }

    @Test
    fun `transfer to the same account is rejected`() {
        assertInvalid(valid.withTransaction(3) { it.copy(toAccountId = 1) })
    }

    @Test
    fun `transfer with a category is rejected`() {
        assertInvalid(valid.withTransaction(3) { it.copy(categoryId = 1) })
    }

    @Test
    fun `category type different from the transaction type is rejected`() {
        assertInvalid(valid.withTransaction(1) { it.copy(categoryId = 2) })
        assertInvalid(valid.withTransaction(2) { it.copy(categoryId = 1) })
        assertInvalid(valid.withTransaction(2) { it.copy(categoryId = 3) })
    }

    @Test
    fun `subcategory with a type different from its parent is rejected`() {
        assertInvalid(valid.withCategory(3) { it.copy(type = CategoryType.INCOME) })
    }

    @Test
    fun `more than two category levels are rejected`() {
        val deeper = CategoryDto(4, "Nivel 3", null, CategoryType.EXPENSE, 3, -65536, "restaurant", false, 0)
        assertInvalid(valid.copy(categories = valid.categories + deeper))
    }

    @Test
    fun `category cycles are rejected`() {
        assertInvalid(valid.withCategory(3) { it.copy(parentId = 3) })
        val cyclic =
            valid.copy(
                categories =
                    listOf(
                        CategoryDto(1, "A", null, CategoryType.EXPENSE, 2, -1, "x", false, 0),
                        CategoryDto(2, "B", null, CategoryType.EXPENSE, 1, -1, "x", false, 0),
                    ),
                transactions = emptyList(),
            )
        assertInvalid(cyclic)
    }

    @Test
    fun `empty or blank account name is rejected`() {
        assertInvalid(valid.withAccount { it.copy(name = "") })
        assertInvalid(valid.withAccount { it.copy(name = "   ") })
    }

    @Test
    fun `account name longer than 40 is rejected and exactly 40 is valid`() {
        assertInvalid(valid.withAccount { it.copy(name = "a".repeat(41)) })
        assertThat(BackupValidator.validate(valid.withAccount { it.copy(name = "a".repeat(40)) })).isNull()
    }

    @Test
    fun `note longer than 200 is rejected and exactly 200 is valid`() {
        assertInvalid(valid.withTransaction(1) { it.copy(note = "n".repeat(201)) })
        assertThat(BackupValidator.validate(valid.withTransaction(1) { it.copy(note = "n".repeat(200)) })).isNull()
    }

    @Test
    fun `category without name and systemKey is rejected`() {
        assertInvalid(valid.withCategory(1) { it.copy(name = null, systemKey = null) })
    }

    @Test
    fun `category with only a system key or only a name is valid`() {
        assertThat(valid.categories.first { it.id == 1L }.name).isNull()
        assertThat(valid.categories.first { it.id == 2L }.systemKey).isNull()
        assertThat(BackupValidator.validate(valid)).isNull()
    }

    @Test
    fun `two active accounts with the same name are rejected, ignoring case and spaces`() {
        assertInvalid(valid.withAccount(1) { it.copy(name = " efectivo ") })
    }

    @Test
    fun `an archived account may repeat an active name`() {
        assertThat(BackupValidator.validate(valid.withAccount(1) { it.copy(name = "Efectivo", isArchived = true) })).isNull()
    }

    @Test
    fun `duplicated category system keys are rejected`() {
        assertInvalid(valid.withCategory(2) { it.copy(name = null, systemKey = "food", type = CategoryType.EXPENSE) })
    }

    @Test
    fun `transactions after the latest allowed date are rejected and that date itself is valid`() {
        val max = LocalDate.of(2027, 9, 28)

        assertThat(BackupValidator.validate(valid.withTransaction(1) { it.copy(date = "2027-09-28") }, max)).isNull()
        assertThat(BackupValidator.validate(valid.withTransaction(1) { it.copy(date = "2027-09-29") }, max))
            .isInstanceOf(DomainError.BackupInvalid::class.java)
    }

    @Test
    fun `invalid dates are rejected`() {
        assertInvalid(valid.withTransaction(1) { it.copy(date = "27/09/2026") })
        assertInvalid(valid.withTransaction(1) { it.copy(date = "2026-13-40") })
        assertInvalid(valid.withTransaction(1) { it.copy(date = "") })
    }

    @Test
    fun `transaction types are covered by the fixture`() {
        assertThat(valid.transactions.map { it.type }.toSet()).containsExactlyElementsIn(TransactionType.entries)
    }

    private val card = validCardBackupDto()

    private fun BackupDto.withCardAccount(change: (AccountDto) -> AccountDto) =
        copy(accounts = accounts.map { if (it.type == AccountType.CREDIT_CARD) change(it) else it })

    private fun BackupDto.withStatement(
        id: Long,
        change: (CardStatementDto) -> CardStatementDto,
    ) = copy(cardStatements = cardStatements.map { if (it.id == id) change(it) else it })

    private fun BackupDto.withCardTransaction(
        id: Long,
        change: (TransactionDto) -> TransactionDto,
    ) = copy(transactions = transactions.map { if (it.id == id) change(it) else it })

    @Test
    fun `negative initial balance is allowed on a credit card`() {
        assertThat(BackupValidator.validate(card)).isNull()
        assertThat(card.accounts.single { it.type == AccountType.CREDIT_CARD }.initialBalance).isLessThan(0)
    }

    @Test
    fun `card without credit limit or with a non-positive or too large limit is rejected`() {
        assertInvalid(card.withCardAccount { it.copy(creditLimit = null) })
        assertInvalid(card.withCardAccount { it.copy(creditLimit = 0) })
        assertInvalid(card.withCardAccount { it.copy(creditLimit = -5) })
        assertInvalid(card.withCardAccount { it.copy(creditLimit = Money.MAX_MINOR_UNITS + 1) })
    }

    @Test
    fun `card with an invalid statement or due day is rejected`() {
        assertInvalid(card.withCardAccount { it.copy(statementDay = null) })
        assertInvalid(card.withCardAccount { it.copy(statementDay = 0) })
        assertInvalid(card.withCardAccount { it.copy(statementDay = 32) })
        assertInvalid(card.withCardAccount { it.copy(paymentDueDay = null) })
        assertInvalid(card.withCardAccount { it.copy(paymentDueDay = 0) })
        assertInvalid(card.withCardAccount { it.copy(paymentDueDay = 32) })
    }

    @Test
    fun `card fields on a non-card account are rejected`() {
        assertInvalid(valid.withAccount { it.copy(creditLimit = 1_000_000) })
        assertInvalid(valid.withAccount { it.copy(statementDay = 15) })
        assertInvalid(valid.withAccount { it.copy(paymentDueDay = 5) })
    }

    @Test
    fun `installments outside 1 to 48 are rejected`() {
        assertInvalid(card.withCardTransaction(4) { it.copy(installments = 0) })
        assertInvalid(card.withCardTransaction(4) { it.copy(installments = 49) })
        assertInvalid(card.withCardTransaction(4) { it.copy(installments = -1) })
    }

    @Test
    fun `installments on income transfer or a non-card expense are rejected`() {
        assertInvalid(card.withCardTransaction(2) { it.copy(installments = 3) })
        assertInvalid(card.withCardTransaction(3) { it.copy(installments = 3) })
        assertInvalid(card.withCardTransaction(1) { it.copy(installments = 3) })
    }

    @Test
    fun `single installment on a card expense is valid`() {
        assertThat(BackupValidator.validate(card.withCardTransaction(4) { it.copy(installments = 1) })).isNull()
        assertThat(BackupValidator.validate(card.withCardTransaction(4) { it.copy(installments = 48) })).isNull()
    }

    @Test
    fun `statement with unknown or non-card account is rejected`() {
        assertInvalid(card.withStatement(1) { it.copy(accountId = 99) })
        assertInvalid(card.withStatement(1) { it.copy(accountId = 1) })
    }

    @Test
    fun `duplicated statement ids and duplicated closings per account are rejected`() {
        assertInvalid(card.copy(cardStatements = card.cardStatements + card.cardStatements.first().copy(id = 99)))
        assertInvalid(card.withStatement(1) { it.copy(id = 2) })
        assertInvalid(card.withStatement(1) { it.copy(closingDate = "2026-09-15") })
    }

    @Test
    fun `statement with non-positive id negative balance or invalid dates is rejected`() {
        assertInvalid(card.withStatement(1) { it.copy(id = 0) })
        assertInvalid(card.withStatement(2) { it.copy(statementBalance = -1) })
        assertInvalid(card.withStatement(2) { it.copy(statementBalance = Money.MAX_MINOR_UNITS + 1) })
        assertInvalid(card.withStatement(1) { it.copy(minimumPayment = -1) })
        assertInvalid(card.withStatement(1) { it.copy(statedInterestAndFees = -1) })
        assertInvalid(card.withStatement(1) { it.copy(closingDate = "15/08/2026") })
        assertInvalid(card.withStatement(1) { it.copy(dueDate = "not-a-date") })
    }

    @Test
    fun `statement with unknown adjustment or one from another account is rejected`() {
        assertInvalid(card.withStatement(1) { it.copy(adjustmentTransactionId = 99) })
        assertInvalid(card.withStatement(1) { it.copy(adjustmentTransactionId = 1) })
    }

    @Test
    fun `statement without adjustment is valid`() {
        assertThat(BackupValidator.validate(card.withStatement(2) { it.copy(adjustmentTransactionId = null) })).isNull()
    }

    private val recurring = validRecurringBackupDto()

    private fun BackupDto.withRecurring(
        id: Long,
        change: (RecurringPaymentDto) -> RecurringPaymentDto,
    ) = copy(recurringPayments = recurringPayments.map { if (it.id == id) change(it) else it })

    @Test
    fun `recurring backup without payments is valid`() {
        val withoutGenerated = recurring.copy(recurringPayments = emptyList(), transactions = validCardBackupDto().transactions)
        assertThat(BackupValidator.validate(withoutGenerated)).isNull()
        assertThat(BackupValidator.validate(valid.copy(recurringPayments = emptyList()))).isNull()
    }

    @Test
    fun `non-positive and duplicated recurring ids are rejected`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(id = 0) })
        assertInvalid(recurring.withRecurring(2) { it.copy(id = -1) })
        assertInvalid(recurring.withRecurring(2) { it.copy(id = 1) })
    }

    @Test
    fun `empty recurring name is rejected and the limit is enforced`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(name = "") })
        assertInvalid(recurring.withRecurring(1) { it.copy(name = "   ") })
        assertInvalid(recurring.withRecurring(1) { it.copy(name = "a".repeat(41)) })
        assertThat(BackupValidator.validate(recurring.withRecurring(1) { it.copy(name = "a".repeat(40)) })).isNull()
    }

    @Test
    fun `transfer type is rejected for recurring payments`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(type = TransactionType.TRANSFER) })
    }

    @Test
    fun `non-positive and too large recurring amounts are rejected`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(amount = 0) })
        assertInvalid(recurring.withRecurring(1) { it.copy(amount = -5) })
        assertInvalid(recurring.withRecurring(1) { it.copy(amount = Money.MAX_MINOR_UNITS + 1) })
        assertThat(
            BackupValidator.validate(recurring.withRecurring(1) { it.copy(amount = Money.MAX_MINOR_UNITS) }),
        ).isNull()
    }

    @Test
    fun `recurring note longer than 200 is rejected and exactly 200 is valid`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(note = "n".repeat(201)) })
        assertThat(
            BackupValidator.validate(recurring.withRecurring(1) { it.copy(note = "n".repeat(200)) }),
        ).isNull()
    }

    @Test
    fun `recurring payment with unknown account or category is rejected`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(accountId = 99) })
        assertInvalid(recurring.withRecurring(1) { it.copy(categoryId = 99) })
    }

    @Test
    fun `recurring payment with a category of the other type is rejected`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(categoryId = 2) })
    }

    @Test
    fun `recurring interval outside 1 to 12 is rejected`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(interval = 0) })
        assertInvalid(recurring.withRecurring(1) { it.copy(interval = 13) })
        assertThat(BackupValidator.validate(recurring.withRecurring(1) { it.copy(interval = 1) })).isNull()
        assertThat(BackupValidator.validate(recurring.withRecurring(1) { it.copy(interval = 12) })).isNull()
    }

    @Test
    fun `recurring reminder outside null or 1 to 7 is rejected`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(reminderDaysBefore = 0) })
        assertInvalid(recurring.withRecurring(1) { it.copy(reminderDaysBefore = 8) })
        assertThat(BackupValidator.validate(recurring.withRecurring(1) { it.copy(reminderDaysBefore = null) })).isNull()
        assertThat(BackupValidator.validate(recurring.withRecurring(1) { it.copy(reminderDaysBefore = 1) })).isNull()
        assertThat(BackupValidator.validate(recurring.withRecurring(1) { it.copy(reminderDaysBefore = 7) })).isNull()
    }

    @Test
    fun `recurring end date before the start date is rejected and equal is valid`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(startDate = "2026-10-15", endDate = "2026-10-14") })
        assertThat(
            BackupValidator.validate(recurring.withRecurring(1) { it.copy(startDate = "2026-10-15", endDate = "2026-10-15") }),
        ).isNull()
        assertThat(BackupValidator.validate(recurring.withRecurring(2) { it.copy(endDate = null) })).isNull()
    }

    @Test
    fun `recurring payment with invalid dates is rejected`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(startDate = "15/09/2026") })
        assertInvalid(recurring.withRecurring(1) { it.copy(nextDueDate = "2026-13-40") })
        assertInvalid(recurring.withRecurring(2) { it.copy(endDate = "not-a-date") })
    }

    @Test
    fun `recurring next due date before the start date is rejected and equal is valid`() {
        assertInvalid(recurring.withRecurring(1) { it.copy(nextDueDate = "2026-09-14") })
        assertThat(
            BackupValidator.validate(recurring.withRecurring(1) { it.copy(nextDueDate = "2026-09-15") }),
        ).isNull()
    }

    @Test
    fun `transaction with an unknown recurring payment is rejected`() {
        assertInvalid(recurring.withTransaction(6) { it.copy(recurringId = 99) })
        assertInvalid(recurring.withTransaction(1) { it.copy(recurringId = 99) })
    }

    @Test
    fun `transaction with a null or existing recurring payment is valid`() {
        assertThat(BackupValidator.validate(recurring.withTransaction(1) { it.copy(recurringId = null) })).isNull()
        assertThat(BackupValidator.validate(recurring.withTransaction(1) { it.copy(recurringId = 2) })).isNull()
        assertThat(BackupValidator.validate(recurring)).isNull()
    }

    private val debtBackup = validDebtBackupDto()

    private fun invalidReason(dto: BackupDto): String? = (BackupValidator.validate(dto) as? DomainError.BackupInvalid)?.reason

    private fun BackupDto.withDebt(
        id: Long,
        change: (DebtDto) -> DebtDto,
    ) = copy(debts = debts.map { if (it.id == id) change(it) else it })

    private fun BackupDto.withPayment(
        id: Long,
        change: (DebtPaymentDto) -> DebtPaymentDto,
    ) = copy(debtPayments = debtPayments.map { if (it.id == id) change(it) else it })

    @Test
    fun `valid debt backup passes and debts are optional`() {
        assertThat(BackupValidator.validate(debtBackup)).isNull()
        assertThat(BackupValidator.validate(debtBackup.copy(debts = emptyList(), debtPayments = emptyList()))).isNull()
    }

    @Test
    fun `non-positive and duplicated debt ids are rejected`() {
        assertInvalid(debtBackup.withDebt(1) { it.copy(id = 0) })
        assertInvalid(debtBackup.withDebt(2) { it.copy(id = 1) })
        assertThat(invalidReason(debtBackup.withDebt(2) { it.copy(id = 1) })).isEqualTo("duplicated debt id")
    }

    @Test
    fun `non-positive and duplicated debt payment ids are rejected`() {
        assertInvalid(debtBackup.withPayment(1) { it.copy(id = 0) })
        assertInvalid(debtBackup.withPayment(2) { it.copy(id = 1) })
        assertThat(invalidReason(debtBackup.withPayment(2) { it.copy(id = 1) })).isEqualTo("duplicated debt payment id")
    }

    @Test
    fun `empty or blank debt name is rejected and the limit is 40 characters`() {
        assertInvalid(debtBackup.withDebt(1) { it.copy(personName = "") })
        assertInvalid(debtBackup.withDebt(1) { it.copy(personName = "   ") })
        assertInvalid(debtBackup.withDebt(1) { it.copy(personName = "a".repeat(41)) })
        assertThat(BackupValidator.validate(debtBackup.withDebt(1) { it.copy(personName = "a".repeat(40)) })).isNull()
    }

    @Test
    fun `debt amount must be between 1 and the maximum`() {
        assertThat(invalidReason(debtBackup.withDebt(1) { it.copy(amount = 0) })).isEqualTo("debt 1: amount must be positive")
        assertInvalid(debtBackup.withDebt(1) { it.copy(amount = -5) })
        assertThat(invalidReason(debtBackup.withDebt(1) { it.copy(amount = Money.MAX_MINOR_UNITS + 1) }))
            .isEqualTo("debt 1: amount too large")
        assertThat(BackupValidator.validate(debtBackup.withDebt(1) { it.copy(amount = Money.MAX_MINOR_UNITS) })).isNull()
    }

    @Test
    fun `invalid debt dates are rejected`() {
        assertThat(invalidReason(debtBackup.withDebt(1) { it.copy(date = "01/08/2026") })).isEqualTo("debt 1: invalid date")
        assertThat(invalidReason(debtBackup.withDebt(1) { it.copy(dueDate = "2026-13-40") })).isEqualTo("debt 1: invalid due date")
    }

    @Test
    fun `due date before the debt date is rejected and the same day is valid`() {
        assertThat(invalidReason(debtBackup.withDebt(1) { it.copy(dueDate = "2026-07-31") }))
            .isEqualTo("debt 1: due date before date")
        assertThat(BackupValidator.validate(debtBackup.withDebt(1) { it.copy(dueDate = "2026-08-01") })).isNull()
    }

    @Test
    fun `debt note longer than 200 is rejected and exactly 200 is valid`() {
        assertInvalid(debtBackup.withDebt(1) { it.copy(note = "n".repeat(201)) })
        assertThat(BackupValidator.validate(debtBackup.withDebt(1) { it.copy(note = "n".repeat(200)) })).isNull()
    }

    @Test
    fun `payment with an unknown debt is rejected`() {
        assertThat(invalidReason(debtBackup.withPayment(1) { it.copy(debtId = 99) }))
            .isEqualTo("debt payment 1: unknown debt 99")
    }

    @Test
    fun `payment amount must be between 1 and the maximum`() {
        assertThat(invalidReason(debtBackup.withPayment(1) { it.copy(amount = 0) }))
            .isEqualTo("debt payment 1: amount must be positive")
        assertInvalid(debtBackup.withPayment(1) { it.copy(amount = -5) })
        assertThat(invalidReason(debtBackup.withPayment(1) { it.copy(amount = Money.MAX_MINOR_UNITS + 1) }))
            .isEqualTo("debt payment 1: amount too large")
    }

    @Test
    fun `payment with an invalid date or a note longer than 200 is rejected`() {
        assertThat(invalidReason(debtBackup.withPayment(2) { it.copy(date = "not-a-date") }))
            .isEqualTo("debt payment 2: invalid date")
        assertInvalid(debtBackup.withPayment(2) { it.copy(note = "n".repeat(201)) })
        assertThat(BackupValidator.validate(debtBackup.withPayment(2) { it.copy(note = "n".repeat(200)) })).isNull()
    }

    @Test
    fun `payment dated before its debt is rejected and the same day is valid`() {
        // Debt 1 starts on 2026-08-01.
        assertThat(invalidReason(debtBackup.withPayment(1) { it.copy(date = "2026-07-31") }))
            .isEqualTo("debt payment 1: date before the debt date")
        assertThat(BackupValidator.validate(debtBackup.withPayment(1) { it.copy(date = "2026-08-01") })).isNull()
    }

    @Test
    fun `payments that add up to more than the debt amount are rejected and the exact amount is valid`() {
        // Debt 1 is 50M and has payments of 10M and 5M: a 40M payment settles it exactly.
        assertThat(BackupValidator.validate(debtBackup.withPayment(2) { it.copy(amount = 40_000_000) })).isNull()
        assertThat(invalidReason(debtBackup.withPayment(2) { it.copy(amount = 40_000_001) }))
            .isEqualTo("debt payment 2: payments exceed the amount of debt 1")
        // Debt 2 is 12M and has one payment of 4M.
        assertInvalid(debtBackup.withPayment(3) { it.copy(amount = 12_000_001) })
    }

    @Test
    fun `a single payment larger than its debt is rejected`() {
        assertThat(invalidReason(debtBackup.withDebt(2) { it.copy(amount = 3_000_000) }))
            .isEqualTo("debt payment 3: payments exceed the amount of debt 2")
    }

    private val transit = validTransitBackupDto()

    private fun BackupDto.withTransitAccount(change: (AccountDto) -> AccountDto) =
        copy(accounts = accounts.map { if (it.type == AccountType.TRANSIT_CARD) change(it) else it })

    @Test
    fun `valid transit backup passes and keeps the fare`() {
        assertThat(BackupValidator.validate(transit)).isNull()
        assertThat(transit.accounts.single { it.type == AccountType.TRANSIT_CARD }.fare).isEqualTo(350_000L)
    }

    @Test
    fun `transit card without fare or with a zero or negative fare is rejected`() {
        assertThat(invalidReason(transit.withTransitAccount { it.copy(fare = null) }))
            .isEqualTo("account 4: transit card without fare")
        assertThat(invalidReason(transit.withTransitAccount { it.copy(fare = 0) })).isEqualTo("account 4: fare must be positive")
        assertThat(invalidReason(transit.withTransitAccount { it.copy(fare = -5) })).isEqualTo("account 4: fare must be positive")
    }

    @Test
    fun `transit fare above the maximum is rejected and the maximum itself is valid`() {
        assertThat(invalidReason(transit.withTransitAccount { it.copy(fare = Money.MAX_MINOR_UNITS + 1) }))
            .isEqualTo("account 4: fare too large")
        assertThat(BackupValidator.validate(transit.withTransitAccount { it.copy(fare = Money.MAX_MINOR_UNITS) })).isNull()
    }

    @Test
    fun `fare on a cash, savings or credit card account is rejected`() {
        assertThat(invalidReason(valid.withAccount { it.copy(fare = 350_000) }))
            .isEqualTo("account 1: fare on a non-transit account")
        assertThat(invalidReason(validCardBackupDto().withCardAccount { it.copy(fare = 350_000) }))
            .isEqualTo("account 3: fare on a non-transit account")
    }

    @Test
    fun `transit card cannot carry card fields`() {
        assertThat(invalidReason(transit.withTransitAccount { it.copy(creditLimit = 1_000_000) }))
            .isEqualTo("account 4: credit limit on a non-card account")
    }

    @Test
    fun `transit card may owe up to one fare as its initial balance`() {
        assertThat(BackupValidator.validate(transit.withTransitAccount { it.copy(initialBalance = -1) })).isNull()
        assertThat(BackupValidator.validate(transit.withTransitAccount { it.copy(initialBalance = -350_000) })).isNull()
    }

    @Test
    fun `transit card owing more than one fare is rejected`() {
        assertThat(invalidReason(transit.withTransitAccount { it.copy(initialBalance = -350_001) }))
            .isEqualTo("account 4: transit credit larger than one fare")
    }

    @Test
    fun `negative initial balance is still rejected on cash and savings accounts`() {
        assertThat(invalidReason(valid.withAccount { it.copy(initialBalance = -1) }))
            .isEqualTo("account 1: negative initial balance")
        assertThat(invalidReason(transit.withTransitAccount { it.copy(type = AccountType.CASH, fare = null, initialBalance = -1) }))
            .isEqualTo("account 4: negative initial balance")
    }

    @Test
    fun `transit card with a zero balance is valid`() {
        assertThat(BackupValidator.validate(transit.withTransitAccount { it.copy(initialBalance = 0) })).isNull()
    }
}
