package com.crguarin.wallet.ui.transactions

import androidx.lifecycle.SavedStateHandle
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class TransactionFormStoreTest {
    @Test
    fun `a new form is not restored and has no fields`() {
        val store = TransactionFormStore(SavedStateHandle())

        assertThat(store.isRestored).isFalse()
        assertThat(store.amountText).isNull()
        assertThat(store.date).isNull()
    }

    @Test
    fun `saved fields come back from the same handle`() {
        val handle = SavedStateHandle()
        TransactionFormStore(handle).save(
            TransactionEditUiState(
                selectedType = TransactionType.INCOME,
                amountText = "1500",
                accountId = 1L,
                categoryId = 4L,
                date = LocalDate.of(2026, 9, 28),
                note = "Venta",
                installments = 3,
            ),
        )

        val restored = TransactionFormStore(handle)

        assertThat(restored.isRestored).isTrue()
        assertThat(restored.type).isEqualTo(TransactionType.INCOME)
        assertThat(restored.amountText).isEqualTo("1500")
        assertThat(restored.accountId).isEqualTo(1L)
        assertThat(restored.toAccountId).isNull()
        assertThat(restored.categoryId).isEqualTo(4L)
        assertThat(restored.date).isEqualTo(LocalDate.of(2026, 9, 28))
        assertThat(restored.note).isEqualTo("Venta")
        assertThat(restored.installments).isEqualTo(3)
    }

    @Test
    fun `setting a field to null removes it`() {
        val handle = SavedStateHandle()
        val store = TransactionFormStore(handle)
        store.toAccountId = 2L

        store.toAccountId = null

        assertThat(handle.contains("tx_form_to_account")).isFalse()
    }

    @Test
    fun `an unknown saved type is ignored`() {
        val store = TransactionFormStore(SavedStateHandle(mapOf("tx_form_type" to "LOAN")))

        assertThat(store.type).isNull()
    }
}
