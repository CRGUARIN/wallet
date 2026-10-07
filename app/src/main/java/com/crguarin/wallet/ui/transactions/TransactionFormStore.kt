package com.crguarin.wallet.ui.transactions

import androidx.lifecycle.SavedStateHandle
import com.crguarin.wallet.domain.model.TransactionType
import java.time.LocalDate

private const val KEY_AMOUNT = "tx_form_amount"
private const val KEY_TYPE = "tx_form_type"
private const val KEY_ACCOUNT = "tx_form_account"
private const val KEY_TO_ACCOUNT = "tx_form_to_account"
private const val KEY_CATEGORY = "tx_form_category"
private const val KEY_DATE = "tx_form_date"
private const val KEY_NOTE = "tx_form_note"
private const val KEY_INSTALLMENTS = "tx_form_installments"

/**
 * The transaction form fields kept in [SavedStateHandle], so an unsaved edit survives process
 * recreation. [TransactionEditViewModel] owns the form state; this class only knows the keys and
 * how each field is stored. Setting a nullable field to null removes it.
 */
internal class TransactionFormStore(
    private val handle: SavedStateHandle,
) {
    /** True when the form comes back after process recreation: the type is always saved with it. */
    val isRestored: Boolean
        get() = handle.contains(KEY_TYPE)

    var type: TransactionType?
        get() = handle.get<String>(KEY_TYPE)?.let { name -> TransactionType.entries.firstOrNull { it.name == name } }
        set(value) = put(KEY_TYPE, value?.name)

    var amountText: String?
        get() = handle[KEY_AMOUNT]
        set(value) = put(KEY_AMOUNT, value)

    var accountId: Long?
        get() = handle[KEY_ACCOUNT]
        set(value) = put(KEY_ACCOUNT, value)

    var toAccountId: Long?
        get() = handle[KEY_TO_ACCOUNT]
        set(value) = put(KEY_TO_ACCOUNT, value)

    var categoryId: Long?
        get() = handle[KEY_CATEGORY]
        set(value) = put(KEY_CATEGORY, value)

    var date: LocalDate?
        get() = handle.get<Long>(KEY_DATE)?.let(LocalDate::ofEpochDay)
        set(value) = put(KEY_DATE, value?.toEpochDay())

    var note: String?
        get() = handle[KEY_NOTE]
        set(value) = put(KEY_NOTE, value)

    var installments: Int?
        get() = handle[KEY_INSTALLMENTS]
        set(value) = put(KEY_INSTALLMENTS, value)

    /** Saves every field of [state]; a field missing from it keeps its saved value. */
    fun save(state: TransactionEditUiState) {
        amountText = state.amountText
        type = state.selectedType
        state.accountId?.let { accountId = it }
        state.toAccountId?.let { toAccountId = it }
        state.categoryId?.let { categoryId = it }
        state.date?.let { date = it }
        note = state.note
        installments = state.installments
    }

    private fun <T> put(
        key: String,
        value: T?,
    ) {
        if (value == null) handle.remove<T>(key) else handle[key] = value
    }
}
