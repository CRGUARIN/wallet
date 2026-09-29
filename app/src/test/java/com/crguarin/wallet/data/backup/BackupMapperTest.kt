package com.crguarin.wallet.data.backup

import com.crguarin.wallet.data.db.TransactionEntity
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
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
}
