package com.crguarin.wallet.data.backup

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.DebtEntity
import com.crguarin.wallet.data.db.DebtPaymentEntity
import com.crguarin.wallet.data.db.RecurringPaymentEntity
import com.crguarin.wallet.data.db.TransactionEntity
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BackupMapperTest {
    private val dto = validBackupDto()

    @Test
    fun `account dto round trips through the entity`() {
        assertThat(dto.accounts.map { it.toEntity().toBackupDto() }).isEqualTo(dto.accounts)
    }

    @Test
    fun `category dto round trips through the entity`() {
        assertThat(dto.categories.map { it.toEntity().toBackupDto() }).isEqualTo(dto.categories)
    }

    @Test
    fun `transaction dto round trips through the entity`() {
        assertThat(dto.transactions.map { it.toEntity().toBackupDto() }).isEqualTo(dto.transactions)
    }

    @Test
    fun `entity ids are preserved`() {
        assertThat(dto.accounts.map { it.toEntity().id }).containsExactly(1L, 2L).inOrder()
        assertThat(dto.categories.map { it.toEntity().id }).containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test
    fun `account timestamps map to epoch millis`() {
        val entity = dto.accounts.first().toEntity()
        assertThat(entity.createdAt).isEqualTo(Instant.ofEpochMilli(1_790_000_000_000))
    }

    @Test
    fun `subcategory keeps its parent and type`() {
        val entity = dto.categories.first { it.id == 3L }.toEntity()
        assertThat(entity.parentId).isEqualTo(1L)
        assertThat(entity.type).isEqualTo(CategoryType.EXPENSE)
    }

    @Test
    fun `card account dto round trips through the entity with card fields`() {
        val card = validCardBackupDto()
        assertThat(card.accounts.map { it.toEntity().toBackupDto() }).isEqualTo(card.accounts)

        val entity = card.accounts.single { it.id == 3L }.toEntity()
        assertThat(entity.creditLimit).isEqualTo(10_000_000L)
        assertThat(entity.statementDay).isEqualTo(15)
        assertThat(entity.paymentDueDay).isEqualTo(5)
        assertThat(entity.initialBalance).isEqualTo(-2_000_000L)
    }

    @Test
    fun `installments round trip through the entity`() {
        val card = validCardBackupDto()
        assertThat(card.transactions.map { it.toEntity().toBackupDto() }).isEqualTo(card.transactions)
        assertThat(
            card.transactions
                .single { it.id == 4L }
                .toEntity()
                .installments,
        ).isEqualTo(3)
    }

    @Test
    fun `card statement dto round trips through the entity`() {
        val card = validCardBackupDto()
        assertThat(card.cardStatements.map { it.toEntity().toBackupDto() }).isEqualTo(card.cardStatements)

        val withAdjustment = card.cardStatements.single { it.id == 2L }.toEntity()
        assertThat(withAdjustment.adjustmentTransactionId).isEqualTo(5L)
        assertThat(withAdjustment.closingDate).isEqualTo(LocalDate.of(2026, 9, 15))

        val withoutAdjustment = card.cardStatements.single { it.id == 1L }.toEntity()
        assertThat(withoutAdjustment.adjustmentTransactionId).isNull()
    }

    @Test
    fun `recurring payment dto round trips through the domain`() {
        val dtos = validRecurringBackupDto().recurringPayments

        assertThat(dtos.map { it.toDomain().toBackupDto() }).isEqualTo(dtos)
    }

    @Test
    fun `recurring domain round trips through the dto`() {
        val payments = listOf(autoMonthly(), confirmYearly())

        assertThat(payments.map { it.toBackupDto().toDomain() }).isEqualTo(payments)
    }

    @Test
    fun `recurring fields map to minor units ISO dates epoch millis and enum names`() {
        val dto = autoMonthly().toBackupDto()

        assertThat(dto.amount).isEqualTo(4_590_000L)
        assertThat(dto.startDate).isEqualTo("2026-09-15")
        assertThat(dto.endDate).isNull()
        assertThat(dto.nextDueDate).isEqualTo("2026-10-15")
        assertThat(dto.createdAt).isEqualTo(1_790_000_000_000L)
        assertThat(dto.frequency).isEqualTo(RecurrenceFrequency.MONTHLY)
        assertThat(dto.mode).isEqualTo(RecurringMode.AUTO)

        val back = dto.toDomain()
        assertThat(back.amount).isEqualTo(Money(4_590_000L))
        assertThat(back.startDate).isEqualTo(LocalDate.of(2026, 9, 15))
        assertThat(back.endDate).isNull()
        assertThat(back.createdAt).isEqualTo(Instant.ofEpochMilli(1_790_000_000_000L))
    }

    @Test
    fun `confirm yearly payment keeps its end date note and reminder`() {
        val dto = confirmYearly().toBackupDto()

        assertThat(dto.endDate).isEqualTo("2028-01-10")
        assertThat(dto.note).isEqualTo("Moto")
        assertThat(dto.reminderDaysBefore).isNull()
        assertThat(dto.mode).isEqualTo(RecurringMode.CONFIRM)
        assertThat(dto.toDomain().endDate).isEqualTo(LocalDate.of(2028, 1, 10))
    }

    @Test
    fun `recurring payment dto round trips through the entity`() {
        val dtos = validRecurringBackupDto().recurringPayments

        assertThat(dtos.map { it.toEntity().toBackupDto() }).isEqualTo(dtos)
    }

    @Test
    fun `recurring entity keeps all fields`() {
        val entity = validRecurringBackupDto().recurringPayments.single { it.id == 2L }.toEntity()

        assertThat(entity).isEqualTo(
            RecurringPaymentEntity(
                id = 2,
                name = "SOAT",
                type = TransactionType.EXPENSE,
                amount = 30_000_000,
                accountId = 2,
                categoryId = 1,
                note = "Moto",
                frequency = RecurrenceFrequency.YEARLY,
                interval = 1,
                startDate = LocalDate.of(2026, 1, 10),
                nextDueDate = LocalDate.of(2027, 1, 10),
                endDate = LocalDate.of(2028, 1, 10),
                mode = RecurringMode.CONFIRM,
                reminderDaysBefore = null,
                isActive = true,
                createdAt = Instant.ofEpochMilli(1_790_000_000_000L),
                updatedAt = Instant.ofEpochMilli(1_790_000_000_000L),
            ),
        )
    }

    @Test
    fun `transaction recurring id round trips through the entity in both directions`() {
        val generated = validRecurringBackupDto().transactions.single { it.id == 6L }

        assertThat(generated.toEntity().recurringId).isEqualTo(1L)
        assertThat(generated.toEntity().toBackupDto()).isEqualTo(generated)

        val manual = validBackupDto().transactions.single { it.id == 1L }
        assertThat(manual.toEntity().recurringId).isNull()
        assertThat(manual.toEntity().toBackupDto()).isEqualTo(manual)
    }

    @Test
    fun `transaction date is written as an ISO date and read back`() {
        val entity =
            TransactionEntity(
                id = 7,
                type = TransactionType.TRANSFER,
                amount = 100,
                accountId = 1,
                toAccountId = 2,
                date = LocalDate.of(2026, 1, 5),
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            )
        val exported = entity.toBackupDto()

        assertThat(exported.date).isEqualTo("2026-01-05")
        assertThat(exported.toEntity()).isEqualTo(entity)
    }

    @Test
    fun `debt dto round trips through the entity`() {
        val debts = validDebtBackupDto().debts

        assertThat(debts.map { it.toEntity().toBackupDto() }).isEqualTo(debts)
    }

    @Test
    fun `debt payment dto round trips through the entity`() {
        val payments = validDebtBackupDto().debtPayments

        assertThat(payments.map { it.toEntity().toBackupDto() }).isEqualTo(payments)
    }

    @Test
    fun `debt entity round trips through the dto`() {
        val entities =
            listOf(
                DebtEntity(
                    id = 1,
                    personName = "Juan Pérez",
                    direction = DebtDirection.OWED_TO_ME,
                    amount = 50_000_000,
                    date = LocalDate.of(2026, 8, 1),
                    dueDate = LocalDate.of(2026, 12, 1),
                    note = "Préstamo para la moto",
                    createdAt = Instant.ofEpochMilli(1_790_000_000_000),
                    updatedAt = Instant.ofEpochMilli(1_790_000_000_001),
                ),
                DebtEntity(
                    id = 2,
                    personName = "María López",
                    direction = DebtDirection.I_OWE,
                    amount = 12_000_000,
                    date = LocalDate.of(2026, 9, 20),
                    createdAt = Instant.ofEpochMilli(1_790_000_000_002),
                    updatedAt = Instant.ofEpochMilli(1_790_000_000_002),
                ),
            )

        assertThat(entities.map { it.toBackupDto().toEntity() }).isEqualTo(entities)
    }

    @Test
    fun `debt payment entity round trips through the dto`() {
        val entities =
            listOf(
                DebtPaymentEntity(
                    id = 1,
                    debtId = 1,
                    amount = 10_000_000,
                    date = LocalDate.of(2026, 9, 1),
                    note = "Primer abono",
                    createdAt = Instant.ofEpochMilli(1_790_000_000_003),
                ),
                DebtPaymentEntity(
                    id = 2,
                    debtId = 2,
                    amount = 4_000_000,
                    date = LocalDate.of(2026, 10, 2),
                    createdAt = Instant.ofEpochMilli(1_790_000_000_004),
                ),
            )

        assertThat(entities.map { it.toBackupDto().toEntity() }).isEqualTo(entities)
    }

    @Test
    fun `debt dates map to ISO dates and the optional fields to null`() {
        val withDue =
            DebtEntity(
                id = 1,
                personName = "Juan",
                direction = DebtDirection.OWED_TO_ME,
                amount = 100,
                date = LocalDate.of(2026, 8, 1),
                dueDate = LocalDate.of(2026, 12, 1),
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ).toBackupDto()
        val withoutDue =
            DebtEntity(
                id = 2,
                personName = "María",
                direction = DebtDirection.I_OWE,
                amount = 100,
                date = LocalDate.of(2026, 9, 20),
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ).toBackupDto()

        assertThat(withDue.date).isEqualTo("2026-08-01")
        assertThat(withDue.dueDate).isEqualTo("2026-12-01")
        assertThat(withoutDue.dueDate).isNull()
        assertThat(withoutDue.note).isNull()
        assertThat(withoutDue.direction).isEqualTo(DebtDirection.I_OWE)
        assertThat(withoutDue.createdAt).isEqualTo(0L)
    }

    private fun autoMonthly() =
        RecurringPayment(
            id = 1,
            name = "Netflix",
            type = TransactionType.EXPENSE,
            amount = Money(4_590_000L),
            accountId = 3,
            categoryId = 1,
            note = null,
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 1,
            startDate = LocalDate.of(2026, 9, 15),
            endDate = null,
            nextDueDate = LocalDate.of(2026, 10, 15),
            mode = RecurringMode.AUTO,
            reminderDaysBefore = 3,
            isActive = true,
            createdAt = Instant.ofEpochMilli(1_790_000_000_000L),
            updatedAt = Instant.ofEpochMilli(1_790_000_000_000L),
        )

    private fun confirmYearly() =
        RecurringPayment(
            id = 2,
            name = "SOAT",
            type = TransactionType.EXPENSE,
            amount = Money(30_000_000L),
            accountId = 2,
            categoryId = 1,
            note = "Moto",
            frequency = RecurrenceFrequency.YEARLY,
            interval = 1,
            startDate = LocalDate.of(2026, 1, 10),
            endDate = LocalDate.of(2028, 1, 10),
            nextDueDate = LocalDate.of(2027, 1, 10),
            mode = RecurringMode.CONFIRM,
            reminderDaysBefore = null,
            isActive = true,
            createdAt = Instant.ofEpochMilli(1_790_000_000_000L),
            updatedAt = Instant.ofEpochMilli(1_790_000_000_000L),
        )
}
