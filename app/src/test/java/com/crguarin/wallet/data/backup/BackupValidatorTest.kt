package com.crguarin.wallet.data.backup

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 3))).isEqualTo(DomainError.BackupVersionUnsupported)
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 0))).isEqualTo(DomainError.BackupVersionUnsupported)
    }

    @Test
    fun `schema versions 1 and 2 are accepted`() {
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 1))).isNull()
        assertThat(BackupValidator.validate(valid.copy(schemaVersion = 2))).isNull()
        assertThat(BackupValidator.validate(validCardBackupDto())).isNull()
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
}
